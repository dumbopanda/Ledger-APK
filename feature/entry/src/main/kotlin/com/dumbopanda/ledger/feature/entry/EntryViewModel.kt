package com.dumbopanda.ledger.feature.entry

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dumbopanda.ledger.core.common.LedgerClock
import com.dumbopanda.ledger.core.data.repository.AccountRepository
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
 * C1's remaining half: wires B9-B13's field components and the C1 data layer onto one real
 * screen (§7.5). Scope note, deliberate, matching BUILD-LOG.md's own C1/C4 split: ADD mode
 * only. Edit mode -- loading an existing entry's fields, the frozen-original/shape-lock rule,
 * the Open/Close leg toggle wired to Delete/Edit -- is C4's job, a separate block; the pieces
 * that already exist for it ([EntryDraft.editModeOriginalIsTransfer], `TxnIdChoice.editFallback`)
 * are there so C4 can wire in without another rework, not because this VM uses them yet.
 */
@HiltViewModel
class EntryViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val historyRepository: HistoryRepository,
    private val entryRepository: EntryRepository,
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

    init {
        viewModelScope.launch {
            combine(accounts, openItems) { accs, items -> accs to items }
                .collect { (accs, items) -> recompute(accs, items) }
        }
    }

    fun onAction(action: EntryAction) {
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

    private fun recompute(accs: List<Account>, items: List<OwedToMeItem>) {
        if (accs.isEmpty()) {
            _uiState.value = EntryUiState.NoAccounts
            return
        }
        if (draft == null) {
            draft = EntryDraft(
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
        }
        publish()
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
        )
    }

    private fun save(draft: EntryDraft) {
        val date = draft.resolvedDate ?: return
        val minor = parseMinorAmount(draft.amount.clean) ?: return
        saving = true
        publish()
        viewModelScope.launch {
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
            saving = false
            this@EntryViewModel.draft = null
            dateText = initialDateText()
            _uiState.value = EntryUiState.Saved
        }
    }

    private fun initialDateText(): TextFieldValue {
        val text = today().format(FULL_DATE_FORMAT)
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
