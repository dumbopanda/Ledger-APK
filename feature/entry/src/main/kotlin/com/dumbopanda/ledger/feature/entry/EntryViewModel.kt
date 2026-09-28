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
import com.dumbopanda.ledger.core.domain.DraftField
import com.dumbopanda.ledger.core.domain.EntryDraft
import com.dumbopanda.ledger.core.domain.EntryDraftValidation
import com.dumbopanda.ledger.core.domain.FormatAmountUseCase
import com.dumbopanda.ledger.core.domain.GenerateTxnIdUseCase
import com.dumbopanda.ledger.core.domain.GroupOwedToMeUseCase
import com.dumbopanda.ledger.core.domain.OwedToMeItem
import com.dumbopanda.ledger.core.domain.ParseDateInputUseCase
import com.dumbopanda.ledger.core.domain.ValidateEntryDraftUseCase
import com.dumbopanda.ledger.core.domain.cleanToMinor
import com.dumbopanda.ledger.core.domain.formatDisplayDate
import com.dumbopanda.ledger.core.domain.formatMoney
import com.dumbopanda.ledger.core.domain.formatMoneyFixed
import com.dumbopanda.ledger.core.model.Account
import com.dumbopanda.ledger.core.model.OPENING_BALANCE
import com.dumbopanda.ledger.core.model.Entry
import com.dumbopanda.ledger.core.model.Money
import com.dumbopanda.ledger.core.model.TxnDirection
import com.dumbopanda.ledger.core.model.TxnFrequency
import com.dumbopanda.ledger.core.model.TxnId
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

// "The day is over when you go to bed, not when the clock rolls over. Until 05:30 an entry is
// still being added to the day that has just finished" (index.html's DAY_STARTS_AT).
private const val DAY_STARTS_AT_MINUTES = 5 * 60 + 30

// Entry's own column limits -- typed input is clipped to these rather than crashing at save.
private const val NAME_MAX = 120
private const val NOTE_MAX = 300
private const val TYPE_MAX = 60
private const val AMOUNT_MAX_INT_DIGITS = 7

/**
 * Add Payment, and edit mode -- "the same form does both jobs" -- behaving as index.html's form
 * does: after a save the form resets (with a toast naming the id) rather than stopping on a
 * "saved" screen; a blocked save toasts why and outlines the field in red; an edit, once saved,
 * sends you back to History.
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

    private fun now() = clock.now().atZone(ZoneId.systemDefault())
    private fun today(): LocalDate = now().toLocalDate()

    // The form's own state -- never derived from the reactive flows below, so another device
    // syncing in never clobbers what's being typed.
    private var draft: EntryDraft? = null
    private var editingOriginal: EditingOriginal? = null
    private var editBannerSub: String? = null
    private var dateText = TextFieldValue("")
    private var quickDate: QuickDate? = null
    private var lateNightDefault = false
    private var nameCustomMode = false
    private var backOld = false
    private var invalid: DraftField? = null
    private var confirm: ConfirmSheetContent? = null
    private var saving = false

    private val accounts: StateFlow<List<Account>> = accountRepository.activeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val openItems: StateFlow<List<OwedToMeItem>> = historyRepository.owedToMeEntries()
        .map { entries -> groupOwedToMe.group(entries).filter { it.isOpen }.sortedByDescending { it.openedDate } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val knownNames: StateFlow<List<String>> = historyRepository.expenseNamesByUse()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow<EntryUiState>(EntryUiState.Loading)
    val uiState: StateFlow<EntryUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EntryEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<EntryEvent> = _events.asSharedFlow()

    init {
        // C15: no ensureSeeded() fallback here anymore -- AccountSetupOverlay (app module) now
        // sits on top of Home itself and unconditionally blocks it for a zero-account user, so
        // this used to race that overlay's own hasAnyAccount() check and could win, silently
        // seeding Domestic/International/Cash before the user ever saw the popup.
        viewModelScope.launch {
            combine(accounts, openItems, knownNames, editRequestStore.request) { accs, _, _, edit -> accs to edit }
                .collect { (accs, edit) -> recompute(accs, edit) }
        }
    }

    fun onAction(action: EntryAction) {
        if (action == EntryAction.CancelEdit) {
            cancelEdit()
            return
        }
        val current = draft ?: return
        when (action) {
            is EntryAction.DateTextChanged -> {
                dateText = action.value
                quickDate = null
                lateNightDefault = false
                draft = current.copy(resolvedDate = resolvedDate(action.value.text))
                clearInvalid(DraftField.DATE)
            }
            EntryAction.DateBlurred -> {
                val state = parseDateInput(dateText.text)
                if (state is DateFieldState.Resolved) dateText = formatted(state.date)
            }
            is EntryAction.DateQuickPicked -> {
                val date = if (action.quick == QuickDate.TODAY) today() else today().minusDays(1)
                dateText = formatted(date)
                quickDate = action.quick
                lateNightDefault = false
                draft = current.copy(resolvedDate = date)
                clearInvalid(DraftField.DATE)
            }
            is EntryAction.ExpenseNameChosen -> {
                nameCustomMode = false
                draft = current.copy(expenseName = action.name)
                clearInvalid(DraftField.NAME)
            }
            EntryAction.AddNewNameChosen -> {
                nameCustomMode = true
                draft = current.copy(expenseName = "")
                clearInvalid(DraftField.NAME)
            }
            is EntryAction.ExpenseNameTyped -> {
                draft = current.copy(expenseName = action.value.take(NAME_MAX))
                clearInvalid(DraftField.NAME)
            }
            is EntryAction.MoreInfoChanged -> draft = current.copy(moreInfo = action.value.take(NOTE_MAX).ifEmpty { null })
            is EntryAction.SelfTransferToggled -> {
                val next = applySelfTransferToggle.apply(current, turningOn = action.on, accounts = accounts.value)
                draft = next
                nameCustomMode = isCustomName(next.expenseName)
            }
            is EntryAction.AccountChanged -> draft = applyAccountChange(current, action.account, AccountSide.FROM)
            is EntryAction.ToAccountChanged -> draft = applyAccountChange(current, action.account, AccountSide.TO)
            is EntryAction.TxnTypeChanged -> draft = current.copy(txnType = action.value.take(TYPE_MAX))
            is EntryAction.SentLegTypeChanged -> draft = current.copy(sentLegType = action.value.take(TYPE_MAX))
            is EntryAction.ReceivedLegTypeChanged -> draft = current.copy(receivedLegType = action.value.take(TYPE_MAX))
            is EntryAction.AmountKeystroke -> {
                val next = amountFormatter.applyKeystroke(current.amount, action.rawText, action.rawCaret)
                if (next == current.amount && overflowsAmount(action.rawText)) {
                    _events.tryEmit(EntryEvent.Toast("Amounts stop at ₹99,99,999"))
                }
                draft = current.copy(amount = next)
                clearInvalid(DraftField.AMOUNT)
            }
            EntryAction.AmountBlurred -> draft = current.copy(amount = padDecimals(current.amount))
            is EntryAction.DirectionChanged -> if (!current.selfTransfer) draft = current.copy(direction = action.direction)
            is EntryAction.BackChanged -> draft = applyBack(current, action.yes)
            is EntryAction.BackKindChanged -> {
                backOld = action.old
                if (current.txnId.isY) draft = applyBackKind(current)
            }
            is EntryAction.OpenItemChosen -> draft = adoptOpenItem(current, action.txnId)
            EntryAction.SaveTapped -> onSaveTapped(current)
            EntryAction.ConfirmDismissed -> confirm = null
            EntryAction.ConfirmConfirmed -> {
                confirm = null
                save(current)
            }
            EntryAction.CancelEdit -> Unit // handled above
        }
        publish()
    }

    private fun recompute(accs: List<Account>, edit: EditRequest?) {
        if (accs.isEmpty()) {
            _uiState.value = EntryUiState.NoAccounts
            return
        }
        if (edit != null) {
            // Tapping Edit always opens the edit form, discarding whatever was mid-typed.
            beginEdit(edit, accs)
            editRequestStore.consume()
        } else if (draft == null) {
            resetForm(accs)
        } else {
            keepAccountsValid(accs)
        }
        publish()
    }

    // The draft outlives the account list: an account archived (or replaced by a sync) while
    // it was picked would otherwise stay "selected" with no chip lit, and be saved against.
    private fun keepAccountsValid(accs: List<Account>) {
        val d = draft ?: return
        val ids = accs.map { it.id }.toSet()
        if (d.accountId in ids && d.toAccountId in ids) return
        val from = if (d.accountId in ids) d.accountId else accs.first().id
        val to = if (d.toAccountId in ids && d.toAccountId != from) d.toAccountId else (accs.firstOrNull { it.id != from } ?: accs.first()).id
        draft = d.copy(accountId = from, toAccountId = to)
    }

    private fun beginEdit(edit: EditRequest, accs: List<Account>) {
        val result = buildEditDraft(edit.entry, edit.sibling, accs)
        draft = result.draft
        editingOriginal = result.original
        dateText = formatted(result.draft.resolvedDate ?: today())
        lateNightDefault = false
        quickDate = null
        nameCustomMode = isCustomName(result.draft.expenseName)
        backOld = false
        invalid = null
        confirm = null
        editBannerSub = "${edit.entry.expenseName} · ${formatDisplayDate(edit.entry.entryDate)} · ₹${formatMoney(edit.entry.amount.minor)}"
    }

    // index.html's fullReset(): the default date (by the 05:30 rule), the first account, No.
    private fun resetForm(accs: List<Account>) {
        val n = now()
        val beforeDayStarts = n.hour * 60 + n.minute < DAY_STARTS_AT_MINUTES
        val date = if (beforeDayStarts) n.toLocalDate().minusDays(1) else n.toLocalDate()
        draft = EntryDraft(
            resolvedDate = date,
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
        dateText = formatted(date)
        quickDate = if (beforeDayStarts) QuickDate.YESTERDAY else QuickDate.TODAY
        lateNightDefault = beforeDayStarts
        editingOriginal = null
        editBannerSub = null
        nameCustomMode = false
        backOld = false
        invalid = null
        confirm = null
    }

    private fun cancelEdit() {
        if (editingOriginal == null) return
        resetForm(accounts.value.ifEmpty { return })
        publish()
        _events.tryEmit(EntryEvent.Toast("Edit cancelled. Nothing changed."))
    }

    private fun onSaveTapped(current: EntryDraft) {
        when (val validation = validateEntryDraft.validate(current)) {
            is EntryDraftValidation.Blocked -> {
                invalid = validation.field
                // The shape-lock message is long -- the web holds it on screen longer.
                _events.tryEmit(EntryEvent.Toast(validation.message, if (validation.field == null) 4600L else 2200L))
                validation.field?.let { _events.tryEmit(EntryEvent.FocusField(it)) }
            }
            EntryDraftValidation.Valid -> {
                val accs = accounts.value
                val account = accs.find { it.id == current.accountId } ?: return
                val toAccount = accs.find { it.id == current.toAccountId } ?: account
                confirm = buildConfirmSheet(current, account, toAccount, backOld && current.txnId.isY, isEditing = editingOriginal != null)
            }
        }
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

    // setBack(): No mints a fresh N; Yes applies whatever "Which one" says.
    private fun applyBack(draft: EntryDraft, yes: Boolean): EntryDraft {
        if (yes == draft.txnId.isY) return draft
        if (!yes) return draft.copy(txnId = generateTxnId.generate(isY = false))
        return applyBackKind(draft.copy(txnId = generateTxnId.generate(isY = true)))
    }

    // applyBackMode(): New mints a fresh Y; Old adopts the first open item (or, with nothing
    // open, falls back to a fresh Y).
    private fun applyBackKind(draft: EntryDraft): EntryDraft {
        if (!backOld) return draft.copy(txnId = generateTxnId.generate(isY = true))
        val first = openItems.value.firstOrNull() ?: return draft.copy(txnId = generateTxnId.generate(isY = true))
        return adoptOpenItem(draft, first.txnId)
    }

    // applyOpenItem(): reuse the item's id, and borrow its name if none is picked yet.
    private fun adoptOpenItem(draft: EntryDraft, txnId: TxnId): EntryDraft {
        val item = openItems.value.find { it.txnId == txnId }
        if (draft.expenseName.isBlank() && item != null) {
            nameCustomMode = isCustomName(item.expenseName)
            return draft.copy(txnId = txnId, expenseName = item.expenseName)
        }
        return draft.copy(txnId = txnId)
    }

    private fun save(current: EntryDraft) {
        val date = current.resolvedDate ?: return
        val minor = cleanToMinor(current.amount.clean) ?: return
        val original = editingOriginal
        val cleaned = current.copy(
            expenseName = current.expenseName.trim(),
            moreInfo = current.moreInfo?.trim()?.ifEmpty { null },
            // A "Custom…" left blank is saved as "Custom", exactly as the web does.
            txnType = current.txnType.trim().ifEmpty { "Custom" },
            sentLegType = current.sentLegType.trim().ifEmpty { "Custom" },
            receivedLegType = current.receivedLegType.trim().ifEmpty { "Custom" },
        )
        saving = true
        publish()
        viewModelScope.launch {
            if (original == null) saveNew(cleaned, date, minor) else saveEdit(cleaned, date, minor, original)
            saving = false
            val id = cleaned.txnId.value
            if (original == null) {
                _events.tryEmit(EntryEvent.Toast(if (cleaned.selfTransfer) "Transfer added" else "Added"))
                _events.tryEmit(EntryEvent.Saved)
            } else {
                _events.tryEmit(EntryEvent.Toast("Updated"))
                _events.tryEmit(EntryEvent.EditSaved)
            }
            accounts.value.takeIf { it.isNotEmpty() }?.let(::resetForm)
            publish()
        }
    }

    private suspend fun saveNew(draft: EntryDraft, date: LocalDate, minor: Long) {
        val createdAt = clock.now()
        if (draft.selfTransfer) {
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
                transferGroup = UUID.randomUUID().toString(),
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

    private fun publish() {
        val d = draft ?: return
        val minor = cleanToMinor(d.amount.clean) ?: 0L
        val verb = if (editingOriginal != null) "Save Changes" else "Add Entry"
        val cta = when {
            minor <= 0L -> verb
            d.selfTransfer -> "$verb • ₹${formatMoneyFixed(minor)} Transfer"
            else -> "$verb • ₹${formatMoneyFixed(minor)} ${if (d.direction == TxnDirection.Debit) "Out" else "In"}"
        }
        _uiState.value = EntryUiState.Ready(
            draft = d,
            dateText = dateText,
            dateFieldState = parseDateInput(dateText.text),
            quickDate = quickDate,
            lateNightDefault = lateNightDefault,
            accounts = accounts.value,
            // "Self Transfer" and "Opening balance" are the app's own names, not ones to pick for a payment.
            knownNames = knownNames.value.filterNot { it == OPENING_BALANCE || (!d.selfTransfer && it == "Self Transfer") },
            nameCustomMode = nameCustomMode,
            openItems = openItems.value,
            backOld = backOld,
            invalid = invalid,
            caption = validateEntryDraft.missingCaption(d),
            ctaText = cta,
            confirm = confirm,
            saving = saving,
            isEditing = editingOriginal != null,
            editBannerSub = editBannerSub,
        )
    }

    private fun clearInvalid(field: DraftField) {
        if (invalid == field) invalid = null
    }

    private fun isCustomName(name: String): Boolean = name.isNotEmpty() && name !in knownNames.value

    private fun resolvedDate(text: String): LocalDate? = (parseDateInput(text) as? DateFieldState.Resolved)?.date

    private fun formatted(date: LocalDate): TextFieldValue {
        val text = date.format(FULL_DATE_FORMAT)
        return TextFieldValue(text, TextRange(text.length))
    }
}

// The keystroke was refused because the whole-rupee part ran past 7 digits (₹99,99,999).
private fun overflowsAmount(raw: String): Boolean {
    val clean = raw.filter { it.isDigit() || it == '.' }
    return clean.substringBefore('.').trimStart('0').length > AMOUNT_MAX_INT_DIGITS
}

// The amount field's blur: "450" -> "450.00", "450.5" -> "450.50", ".5" -> "0.50".
private fun padDecimals(amount: AmountFieldValue): AmountFieldValue {
    val clean = amount.clean
    if (clean.isEmpty()) return amount
    // A zero is no amount: padding it to "0.00" left a field that typing garbled (the caret
    // landed among the zeros), so it clears back to empty instead.
    if (clean.toDoubleOrNull() == 0.0) return AmountFieldValue("", 0)
    val padded = if ('.' in clean) {
        val whole = clean.substringBefore('.').ifEmpty { "0" }
        val frac = (clean.substringAfter('.') + "00").take(2)
        "$whole.$frac"
    } else {
        "$clean.00"
    }
    return AmountFieldValue(padded, padded.length)
}
