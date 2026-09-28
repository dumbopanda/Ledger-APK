package com.dumbopanda.ledger.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dumbopanda.ledger.core.common.LedgerClock
import com.dumbopanda.ledger.core.data.repository.AccountRepository
import com.dumbopanda.ledger.core.data.repository.EditRequestStore
import com.dumbopanda.ledger.core.data.repository.EntryRepository
import com.dumbopanda.ledger.core.data.repository.EntrySyncMark
import com.dumbopanda.ledger.core.data.repository.HistoryRepository
import com.dumbopanda.ledger.core.data.repository.SyncStatusRepository
import com.dumbopanda.ledger.core.domain.GroupEntriesByDayUseCase
import com.dumbopanda.ledger.core.domain.GroupOwedToMeUseCase
import com.dumbopanda.ledger.core.domain.OwedToMeItem
import com.dumbopanda.ledger.core.model.Account
import com.dumbopanda.ledger.core.model.Entry
import com.dumbopanda.ledger.core.model.isOpeningBalance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

// "40 rows, then 'Show earlier'" -- and 40 more per tap.
private const val PAGE = 40

/**
 * History: Recent (one month at a time, driven by the month toggle it shares with Stats) and
 * Owed to me (every Y id, open or closed, regardless of month -- as on the web), plus the
 * entry-detail sheet both of them open.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    accountRepository: AccountRepository,
    private val entryRepository: EntryRepository,
    private val editRequestStore: EditRequestStore,
    syncStatusRepository: SyncStatusRepository,
    private val groupOwedToMe: GroupOwedToMeUseCase,
    private val groupByDay: GroupEntriesByDayUseCase,
    private val clock: LedgerClock,
) : ViewModel() {

    private fun today(): LocalDate = clock.now().atZone(ZoneId.systemDefault()).toLocalDate()

    private val month = MutableStateFlow(YearMonth.from(today()))

    private val accounts: StateFlow<List<Account>> = accountRepository.allAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Opening balances are set and shown in Settings > Accounts, not listed as things that happened.
    private val monthEntries: StateFlow<List<Entry>> = month.flatMapLatest { historyRepository.entriesInMonth(it) }
        .map { entries -> entries.filterNot { it.isOpeningBalance } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Every live Y leg, raw -- the Owed-to-me list and the detail sheet's Open/Close toggle,
    // which needs the OTHER leg's full Entry.
    private val owedEntries: StateFlow<List<Entry>> = historyRepository.owedToMeEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val owedItems: StateFlow<List<OwedToMeItem>> = owedEntries.map { groupOwedToMe.group(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val syncMarks: StateFlow<Map<String, EntrySyncMark>> = syncStatusRepository.entrySyncMarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private var tab = HistoryTab.RECENT
    private var shown = PAGE
    private var detail: EntryDetailState? = null

    private val _uiState = MutableStateFlow(publishState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(accounts, monthEntries, owedItems, syncMarks) { _, _, _, _ -> }.collect { publish() }
        }
    }

    /** The shared month toggle moved (0 = this month, negative = further back). */
    fun setMonthOffset(offset: Int) {
        val target = YearMonth.from(today()).plusMonths(offset.toLong())
        if (target == month.value) return
        month.value = target
        shown = PAGE
        publish()
    }

    fun onAction(action: HistoryAction) {
        when (action) {
            is HistoryAction.TabSelected -> tab = action.tab
            HistoryAction.ShowEarlierTapped -> shown += PAGE
            is HistoryAction.EntryTapped -> openDetail(action.entry)
            is HistoryAction.OwedItemTapped -> {
                val item = action.item
                val targetId = if (item.isOpen) item.openLegEntryId else (item.closeLegEntryId ?: item.openLegEntryId)
                owedEntries.value.find { it.id == targetId }?.let(::openDetail)
            }
            HistoryAction.DetailDismissed -> detail = null
            is HistoryAction.LegToggled -> toggleLeg(action.showOpen)
            HistoryAction.DeleteTapped -> detail = detail?.copy(showDeleteConfirm = true)
            HistoryAction.DeleteCancelled -> detail = detail?.copy(showDeleteConfirm = false)
            HistoryAction.DeleteConfirmed -> confirmDelete()
            HistoryAction.EditTapped -> requestEdit()
        }
        publish()
    }

    private fun openDetail(entry: Entry) {
        // The Open/Close toggle is for a loan and its repayment -- never a self transfer's legs.
        val owedItem = if (entry.txnId.isY && entry.transferGroup == null) owedItems.value.find { it.txnId == entry.txnId } else null
        detail = EntryDetailState(
            entry = entry,
            sibling = null,
            owedToMeItem = owedItem,
            showingOpenLeg = owedItem == null || entry.id != owedItem.closeLegEntryId,
            showDeleteConfirm = false,
            deleting = false,
        )
        val group = entry.transferGroup ?: return
        viewModelScope.launch {
            val sibling = entryRepository.transferLegs(group).find { it.id != entry.id } ?: return@launch
            if (detail?.entry?.id != entry.id) return@launch // the sheet moved on before this resolved
            detail = detail?.copy(sibling = sibling)
            publish()
        }
    }

    private fun toggleLeg(showOpen: Boolean) {
        val current = detail ?: return
        val item = current.owedToMeItem ?: return
        val targetId = if (showOpen) item.openLegEntryId else (item.closeLegEntryId ?: return)
        val leg = owedEntries.value.find { it.id == targetId } ?: return
        detail = current.copy(entry = leg, showingOpenLeg = showOpen)
    }

    // Hands the displayed leg (whichever side the toggle selects) to the Add Payment form.
    private fun requestEdit() {
        val entry = detail?.entry ?: return
        val group = entry.transferGroup
        detail = null
        if (group == null) {
            editRequestStore.request(entry, sibling = null)
        } else {
            viewModelScope.launch {
                editRequestStore.request(entry, entryRepository.transferLegs(group).find { it.id != entry.id })
            }
        }
    }

    private fun confirmDelete() {
        val current = detail ?: return
        detail = current.copy(deleting = true)
        viewModelScope.launch {
            entryRepository.deleteEntry(current.entry.id)
            detail = null
            publish()
            _events.tryEmit("Deleted")
        }
    }

    private fun publish() {
        _uiState.value = publishState()
    }

    // Every flow read here is declared above `_uiState`, so all are initialised by the time its
    // initialiser first calls this.
    private fun publishState(): HistoryUiState {
        val entries = monthEntries.value
        val accs = accounts.value
        val items = owedItems.value
        return HistoryUiState(
            tab = tab,
            accounts = accs.associateBy { it.id },
            accountOrder = accs.mapIndexed { index, account -> account.id to index }.toMap(),
            month = month.value,
            today = today(),
            dayGroups = groupByDay.group(entries.take(shown)),
            recentCount = entries.distinctBy { it.transferGroup ?: it.id }.size, // a transfer is one thing that happened, as in Stats
            hasMore = entries.size > shown,
            syncMarks = syncMarks.value,
            owedToMeOpen = items.filter { it.isOpen }.sortedByDescending { it.openedDate },
            owedToMeClosed = items.filter { !it.isOpen }.sortedByDescending { it.closedDate },
            detail = detail,
        )
    }
}
