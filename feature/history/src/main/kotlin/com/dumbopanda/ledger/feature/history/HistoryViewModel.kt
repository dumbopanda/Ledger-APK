package com.dumbopanda.ledger.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.dumbopanda.ledger.core.common.LedgerClock
import com.dumbopanda.ledger.core.data.repository.AccountRepository
import com.dumbopanda.ledger.core.data.repository.EditRequestStore
import com.dumbopanda.ledger.core.data.repository.EntryRepository
import com.dumbopanda.ledger.core.data.repository.HistoryRepository
import com.dumbopanda.ledger.core.domain.GroupOwedToMeUseCase
import com.dumbopanda.ledger.core.domain.MonthSummary
import com.dumbopanda.ledger.core.domain.OwedToMeItem
import com.dumbopanda.ledger.core.model.Account
import com.dumbopanda.ledger.core.model.Entry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

/**
 * §7.7 (Recent) + §7.8 (Owed to me) + §7.10/§7.11 (the shared entry-detail sheet, reached from
 * either tab). [pagedEntries] is kept OUTSIDE [uiState] on purpose -- the standard Paging3 +
 * Compose split (see `feature:entry`'s draft/flows split for the equivalent reasoning): a
 * `PagingData` isn't a value [combine] can usefully compare, so it's its own `Flow`, collected
 * separately via `collectAsLazyPagingItems()`.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val accountRepository: AccountRepository,
    private val entryRepository: EntryRepository,
    private val editRequestStore: EditRequestStore,
    private val groupOwedToMe: GroupOwedToMeUseCase,
    private val clock: LedgerClock,
) : ViewModel() {

    val pagedEntries: Flow<PagingData<Entry>> = historyRepository.pagedEntries().cachedIn(viewModelScope)

    private fun currentMonth(): YearMonth = YearMonth.from(clock.now().atZone(ZoneId.systemDefault()))

    private val accounts: StateFlow<List<Account>> = accountRepository.activeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Every live Y-leg, raw -- source for both owedToMeItems (grouped) and the detail sheet's
    // Open/Close toggle, which needs the OTHER leg's full Entry, not just its OwedToMeItem
    // summary. One reactive source, two derived uses, instead of a second repository round trip.
    private val owedToMeEntries: StateFlow<List<Entry>> = historyRepository.owedToMeEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val owedToMeItems: StateFlow<List<OwedToMeItem>> = owedToMeEntries
        .map { groupOwedToMe.group(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val monthOutIn: StateFlow<Pair<Long, Long>> = historyRepository.monthOutIn(currentMonth())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L to 0L)

    private var tab = HistoryTab.RECENT
    private var detail: EntryDetailState? = null

    private val _uiState = MutableStateFlow(emptyUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(accounts, owedToMeItems, monthOutIn) { accs, items, outIn -> Triple(accs, items, outIn) }
                .collect { (accs, items, outIn) -> publish(accs, items, outIn) }
        }
    }

    fun onAction(action: HistoryAction) {
        when (action) {
            is HistoryAction.TabSelected -> tab = action.tab
            is HistoryAction.EntryTapped -> {
                openDetail(action.entry)
                if (action.entry.transferGroup != null) resolveToAccount(action.entry)
            }
            is HistoryAction.OwedItemTapped -> {
                val targetId = if (action.item.isOpen) action.item.openLegEntryId else (action.item.closeLegEntryId ?: action.item.openLegEntryId)
                owedToMeEntries.value.find { it.id == targetId }?.let(::openDetail)
            }
            HistoryAction.DetailDismissed -> detail = null
            is HistoryAction.LegToggled -> toggleLeg(action.showOpen)
            HistoryAction.DeleteTapped -> detail = detail?.copy(showDeleteConfirm = true)
            HistoryAction.DeleteCancelled -> detail = detail?.copy(showDeleteConfirm = false)
            HistoryAction.DeleteConfirmed -> confirmDelete()
            HistoryAction.EditTapped -> requestEdit()
        }
        publish(accounts.value, owedToMeItems.value, monthOutIn.value)
    }

    private fun openDetail(entry: Entry) {
        val accs = accounts.value.associateBy { it.id }
        val owedItem = if (entry.txnId.isY) owedToMeItems.value.find { it.txnId == entry.txnId } else null
        val showingOpen = owedItem == null || entry.id == owedItem.openLegEntryId
        detail = EntryDetailState(
            entry = entry,
            account = accs[entry.accountId],
            toAccount = null,
            owedToMeItem = owedItem,
            showingOpenLeg = showingOpen,
            showDeleteConfirm = false,
            deleteMessage = deleteMessageFor(entry, owedItem),
            deleting = false,
        )
    }

    private fun resolveToAccount(entry: Entry) {
        val group = entry.transferGroup ?: return
        viewModelScope.launch {
            val sibling = entryRepository.transferLegs(group).find { it.id != entry.id } ?: return@launch
            if (detail?.entry?.id != entry.id) return@launch // sheet moved on before this resolved
            val accs = accounts.value.associateBy { it.id }
            detail = detail?.copy(toAccount = accs[sibling.accountId])
            publish(accounts.value, owedToMeItems.value, monthOutIn.value)
        }
    }

    private fun toggleLeg(showOpen: Boolean) {
        val current = detail ?: return
        val item = current.owedToMeItem ?: return
        val targetId = if (showOpen) item.openLegEntryId else (item.closeLegEntryId ?: return)
        val sibling = owedToMeEntries.value.find { it.id == targetId } ?: return
        val accs = accounts.value.associateBy { it.id }
        detail = current.copy(
            entry = sibling,
            account = accs[sibling.accountId],
            showingOpenLeg = showOpen,
            deleteMessage = deleteMessageFor(sibling, item),
        )
    }

    private fun deleteMessageFor(entry: Entry, owedItem: OwedToMeItem?) = buildDeleteConfirmMessage(
        amount = entry.amount,
        expenseName = entry.expenseName,
        date = entry.entryDate,
        isTransferLeg = entry.transferGroup != null,
        openTxnId = if (owedItem != null && owedItem.isOpen) entry.txnId else null,
    )

    // §7.6/C4: hands the currently-displayed leg (already whichever side the Open/Close
    // toggle selects) to EntryViewModel via EditRequestStore. A transfer leg's sibling is
    // resolved fresh here rather than reused from `resolveToAccount`'s result, since that one
    // only kept the sibling's Account, not its full Entry.
    private fun requestEdit() {
        val entry = detail?.entry ?: return
        val group = entry.transferGroup
        detail = null // the sheet is done its job -- the form takes over from here
        if (group == null) {
            editRequestStore.request(entry, sibling = null)
        } else {
            viewModelScope.launch {
                val sibling = entryRepository.transferLegs(group).find { it.id != entry.id }
                editRequestStore.request(entry, sibling)
            }
        }
    }

    private fun confirmDelete() {
        val current = detail ?: return
        detail = current.copy(deleting = true)
        publish(accounts.value, owedToMeItems.value, monthOutIn.value)
        viewModelScope.launch {
            entryRepository.deleteEntry(current.entry.id)
            detail = null
            publish(accounts.value, owedToMeItems.value, monthOutIn.value)
        }
    }

    private fun publish(accs: List<Account>, items: List<OwedToMeItem>, outIn: Pair<Long, Long>) {
        _uiState.value = HistoryUiState(
            tab = tab,
            accounts = accs.associateBy { it.id },
            monthSummary = MonthSummary(outMinor = outIn.first, inMinor = outIn.second),
            owedToMeOpen = items.filter { it.isOpen }.sortedByDescending { it.openedDate },
            owedToMeClosed = items.filter { !it.isOpen }.sortedByDescending { it.closedDate },
            detail = detail,
        )
    }

    private fun emptyUiState() = HistoryUiState(
        tab = HistoryTab.RECENT,
        accounts = emptyMap(),
        monthSummary = MonthSummary(0, 0),
        owedToMeOpen = emptyList(),
        owedToMeClosed = emptyList(),
        detail = null,
    )
}
