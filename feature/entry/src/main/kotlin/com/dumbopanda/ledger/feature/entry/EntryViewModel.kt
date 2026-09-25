package com.dumbopanda.ledger.feature.entry

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dumbopanda.ledger.core.common.LedgerClock
import com.dumbopanda.ledger.core.data.repository.AccountRepository
import com.dumbopanda.ledger.core.data.repository.EditRequest
import com.dumbopanda.ledger.core.data.repository.EditRequestStore
import com.dumbopanda.ledger.core.data.repository.EntryRepository
import com.dumbopanda.ledger.core.data.repository.HistoryRepository
import com.dumbopanda.ledger.core.domain.AmountFieldValue
import com.dumbopanda.ledger.core.domain.ApplySelfTransferToggleUseCase
import com.dumbopanda.ledger.core.domain.DateFieldState
import com.dumbopanda.ledger.core.domain.EntryDraft
import com.dumbopanda.ledger.core.domain.EntryDraftValidation
import com.dumbopanda.ledger.core.domain.FormatAmountUseCase
import com.dumbopanda.ledger.core.domain.GenerateTxnIdUseCase
import com.dumbopanda.ledger.core.domain.GroupOwedToMeUseCase
import com.dumbopanda.ledger.core.domain.OwedToMeItem
import com.dumbopanda.ledger.core.domain.ParseDateInputUseCase
import com.dumbopanda.ledger.core.domain.ValidateEntryDraftUseCase
import com.dumbopanda.ledger.core.model.Account
import com.dumbopanda.ledger.core.model.Entry
import com.dumbopanda.ledger.core.model.Money
import com.dumbopanda.ledger.core.model.TxnDirection
import com.dumbopanda.ledger.core.model.TxnFrequency
import com.dumbopanda.ledger.feature.entry.component.AccountSide
import com.dumbopanda.ledger.feature.entry.component.TXN_TYPE_OPTIONS
import com.dumbopanda.ledger.feature.entry.component.resolveAccountSwap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

private val FULL_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/**
 * C1's screen assembly (§7.5) plus C4's edit mode (§7.6). Add and Edit are "the same form
 * doing both jobs" per §7.6's own words -- [draft]/[save] don't branch on a mode flag, they
 * branch on whether [editingOriginal] is set, which is exactly the "frozen at the moment
 * editing starts" rows §7.6 calls for.
 *
 * Edit is entered via [EditRequestStore] -- observed reactively (not just once at init), since
 * this ViewModel can outlive a single History->Edit round trip. See [EditRequestStore]'s own
 * doc for why a singleton store exists at all instead of a navigation argument.
 */
@HiltViewModel
class EntryViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val historyRepository: HistoryRepository,
    private val entryRepository: EntryRepository,
    private val editRequestStore: EditRequestStore,
    private val parseDateInput: ParseDateInputUseCase,
    private val applySelfTransferToggle: ApplySelfTransferToggleUseCase,
    private val validateEntryDraft: ValidateEntryDraftUseCase,
    private val generateTxnId: GenerateTxnIdUseCase,
    private val groupOwedToMe: GroupOwedToMeUseCase,
    private val amountFormatter: FormatAmountUseCase,
    private val clock: LedgerClock,
) : ViewModel() {

    private fun today(): LocalDate = clock.now().atZone(ZoneId.systemDefault()).toLocalDate()

    // The form's own state -- not derived from the reactive flows below, so a fresh accounts/
    // openItems emission (another device syncing in, say) never clobbers what's being typed.
    private var draft: EntryDraft? = null
    private var editingOriginal: EditingOriginal? = null
    private var dateText: TextFieldValue = initialDateText()
    private var showConfirmSheet = false
    private var saving = false

    private val accounts: StateFlow<List<Account>> = accountRepository.activeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val openItems: StateFlow<List<OwedToMeItem>> = historyRepository.owedToMeEntries()
        .map { entries -> groupOwedToMe.group(entries).filter { it.isOpen } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow<EntryUiState>(EntryUiState.Loading)
    val uiState: StateFlow<EntryUiState> = _uiState.asStateFlow()

    // One-shot events (§7.6's "Edit cancelled — nothing changed" toast) -- not form state, so
    // not part of EntryUiState, which the Route would otherwise re-show on every recomposition.
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(accounts, openItems, editRequestStore.request) { accs, items, edit -> Triple(accs, items, edit) }
                .collect { (accs, items, edit) -> recompute(accs, items, edit) }
        }
    }

    fun onAction(action: EntryAction) {
        if (action == EntryAction.CancelEdit) {
            cancelEdit()
            return
        }
        val current = draft ?: return
        draft = when (action) {
            is EntryAction.DateTextChanged -> {
                dateText = action.value
                val state = parseDateInput(action.value.text)
                current.copy(resolvedDate = (state as? DateFieldState.Resolved)?.date)
            }
            is EntryAction.ExpenseNameChanged -> current.copy(expenseName = action.value)
            is EntryAction.MoreInfoChanged -> current.copy(moreInfo = action.value.ifBlank { null })
            is EntryAction.SelfTransferToggled ->
                applySelfTransferToggle.apply(current, turningOn = action.on, accounts = accounts.value)
            is EntryAction.AccountChanged -> applyAccountChange(current, action.account, side = AccountSide.FROM)
            is EntryAction.ToAccountChanged -> applyAccountChange(current, action.account, side = AccountSide.TO)
            is EntryAction.TxnTypeChanged -> current.copy(txnType = action.value)
            is EntryAction.SentLegTypeChanged -> current.copy(sentLegType = action.value)
            is EntryAction.ReceivedLegTypeChanged -> current.copy(receivedLegType = action.value)
            is EntryAction.AmountKeystroke ->
                current.copy(amount = amountFormatter.applyKeystroke(current.amount, action.rawText, action.rawCaret))
            is EntryAction.DirectionChanged -> current.copy(direction = action.direction)
            is EntryAction.TxnIdChanged -> current.copy(txnId = action.txnId)
            EntryAction.SaveTapped -> {
                if (validateEntryDraft.validate(current) == EntryDraftValidation.Valid) showConfirmSheet = true
                current
            }
            EntryAction.ConfirmDismissed -> {
                showConfirmSheet = false
                current
            }
            EntryAction.ConfirmConfirmed -> {
                showConfirmSheet = false
                save(current)
                current
            }
            EntryAction.CancelEdit -> current // unreachable, handled above
        }
        publish()
    }

    private fun applyAccountChange(draft: EntryDraft, changed: Account, side: AccountSide): EntryDraft {
        if (!draft.selfTransfer) {
            return if (side == AccountSide.FROM) draft.copy(accountId = changed.id) else draft.copy(toAccountId = changed.id)
        }
        val from = accounts.value.find { it.id == draft.accountId } ?: return draft
        val to = accounts.value.find { it.id == draft.toAccountId } ?: return draft
        val (newFrom, newTo) = resolveAccountSwap(from, to, side, changed)
        return draft.copy(accountId = newFrom.id, toAccountId = newTo.id)
    }

    private fun recompute(accs: List<Account>, items: List<OwedToMeItem>, edit: EditRequest?) {
        if (accs.isEmpty()) {
            _uiState.value = EntryUiState.NoAccounts
            return
        }
        if (edit != null) {
            // Tapping Edit always opens the edit form, discarding whatever was mid-typed in
            // Add -- the whole screen just swapped jobs, same as §7.6 describes.
            val result = buildEditDraft(edit.entry, edit.sibling, accs)
            draft = result.draft
            editingOriginal = result.original
            dateText = dateTextFor(result.draft.resolvedDate)
            showConfirmSheet = false
            editRequestStore.consume()
        } else if (draft == null) {
            draft = buildAddDraft(accs)
        }
        publish()
    }

    private fun buildAddDraft(accs: List<Account>) = EntryDraft(
        resolvedDate = today(),
        expenseName = "",
        moreInfo = null,
        selfTransfer = false,
        accountId = accs.first().id,
        toAccountId = accs.getOrElse(1) { accs.first() }.id,
        txnId = generateTxnId.generate(isY = false),
        txnType = TXN_TYPE_OPTIONS.first(),
        sentLegType = "Sent Transfer",
        receivedLegType = "Received Transfer",
        amount = AmountFieldValue("", 0),
        direction = TxnDirection.Debit,
        editModeOriginalIsTransfer = null,
    )

    private fun cancelEdit() {
        if (editingOriginal == null) return // Cancel only means something mid-edit
        draft = null
        editingOriginal = null
        showConfirmSheet = false
        dateText = initialDateText()
        recompute(accounts.value, openItems.value, edit = null)
        _events.tryEmit("Edit cancelled — nothing changed")
    }

    private fun publish() {
        val d = draft ?: return
        val validation = validateEntryDraft.validate(d)
        val accs = accounts.value
        val account = accs.find { it.id == d.accountId } ?: accs.firstOrNull()
        val toAccount = accs.find { it.id == d.toAccountId } ?: account
        val sentence = if (validation == EntryDraftValidation.Valid && account != null && toAccount != null) {
            buildConfirmSentence(d.amount, d.direction, requireNotNull(d.resolvedDate), d.expenseName, account, d.selfTransfer, toAccount)
        } else {
            ""
        }
        _uiState.value = EntryUiState.Ready(
            draft = d,
            dateText = dateText,
            dateFieldState = parseDateInput(dateText.text),
            accounts = accs,
            openItems = openItems.value,
            blockedMessage = (validation as? EntryDraftValidation.Blocked)?.message,
            showConfirmSheet = showConfirmSheet,
            confirmSentence = sentence,
            saving = saving,
            isEditing = editingOriginal != null,
        )
    }

    private fun save(draft: EntryDraft) {
        val date = draft.resolvedDate ?: return
        val minor = parseMinorAmount(draft.amount.clean) ?: return
        val original = editingOriginal
        saving = true
        publish()
        viewModelScope.launch {
            if (original == null) {
                saveNew(draft, date, minor)
            } else {
                saveEdit(draft, date, minor, original)
            }
            saving = false
            this@EntryViewModel.draft = null
            this@EntryViewModel.editingOriginal = null
            dateText = initialDateText()
            _uiState.value = EntryUiState.Saved
        }
    }

    private suspend fun saveNew(draft: EntryDraft, date: LocalDate, minor: Long) {
        val createdAt = clock.now()
        if (draft.selfTransfer) {
            val transferGroup = UUID.randomUUID().toString()
            val sent = Entry(
                id = UUID.randomUUID().toString(),
                clientUid = UUID.randomUUID().toString(),
                entryDate = date,
                txnId = draft.txnId,
                accountId = draft.accountId,
                expenseName = draft.expenseName,
                moreInfo = draft.moreInfo,
                txnType = draft.sentLegType,
                txnFreq = TxnFrequency.Monthly,
                amount = Money(minor),
                direction = TxnDirection.Debit,
                transferGroup = transferGroup,
                createdAt = createdAt,
            )
            val received = sent.copy(
                id = UUID.randomUUID().toString(),
                clientUid = UUID.randomUUID().toString(),
                accountId = draft.toAccountId,
                txnType = draft.receivedLegType,
                direction = TxnDirection.Credit,
            )
            entryRepository.addTransfer(sent, received)
        } else {
            entryRepository.addEntry(
                Entry(
                    id = UUID.randomUUID().toString(),
                    clientUid = UUID.randomUUID().toString(),
                    entryDate = date,
                    txnId = draft.txnId,
                    accountId = draft.accountId,
                    expenseName = draft.expenseName,
                    moreInfo = draft.moreInfo,
                    txnType = draft.txnType,
                    txnFreq = TxnFrequency.Monthly,
                    amount = Money(minor),
                    direction = draft.direction,
                    transferGroup = null,
                    createdAt = createdAt,
                ),
            )
        }
    }

    private suspend fun saveEdit(draft: EntryDraft, date: LocalDate, minor: Long, original: EditingOriginal) {
        when (original) {
            is EditingOriginal.Plain -> entryRepository.updateEntry(
                Entry(
                    id = original.id,
                    clientUid = original.clientUid,
                    entryDate = date,
                    txnId = draft.txnId,
                    accountId = draft.accountId,
                    expenseName = draft.expenseName,
                    moreInfo = draft.moreInfo,
                    txnType = draft.txnType,
                    txnFreq = TxnFrequency.Monthly,
                    amount = Money(minor),
                    direction = draft.direction,
                    transferGroup = null,
                    createdAt = original.createdAt,
                ),
            )
            is EditingOriginal.Transfer -> {
                val debit = Entry(
                    id = original.debitId,
                    clientUid = original.debitClientUid,
                    entryDate = date,
                    txnId = draft.txnId,
                    accountId = draft.accountId,
                    expenseName = draft.expenseName,
                    moreInfo = draft.moreInfo,
                    txnType = draft.sentLegType,
                    txnFreq = TxnFrequency.Monthly,
                    amount = Money(minor),
                    direction = TxnDirection.Debit,
                    transferGroup = original.transferGroup,
                    createdAt = original.debitCreatedAt,
                )
                val credit = debit.copy(
                    id = original.creditId,
                    clientUid = original.creditClientUid,
                    accountId = draft.toAccountId,
                    txnType = draft.receivedLegType,
                    direction = TxnDirection.Credit,
                    createdAt = original.creditCreatedAt,
                )
                entryRepository.updateTransfer(debit, credit)
            }
        }
    }

    private fun initialDateText(): TextFieldValue = dateTextFor(null)

    private fun dateTextFor(date: LocalDate?): TextFieldValue {
        val text = (date ?: today()).format(FULL_DATE_FORMAT)
        return TextFieldValue(text, TextRange(text.length))
    }
}

// "500" -> 50000L (paise), "500.5" -> 50050L. Every clean string reaching here already passed
// ValidateEntryDraftUseCase's isPositive check, so it is well-formed by construction.
private fun parseMinorAmount(clean: String): Long? {
    if (clean.isBlank()) return null
    val dotIndex = clean.indexOf('.')
    val integerPart = if (dotIndex == -1) clean else clean.substring(0, dotIndex)
    val decimalPart = if (dotIndex == -1) "" else clean.substring(dotIndex + 1).padEnd(2, '0').take(2)
    val rupees = integerPart.ifEmpty { "0" }.toLongOrNull() ?: return null
    val paise = decimalPart.ifEmpty { "0" }.toLongOrNull() ?: return null
    return rupees * 100 + paise
}
