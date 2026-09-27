package com.dumbopanda.ledger.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dumbopanda.ledger.core.common.LedgerClock
import com.dumbopanda.ledger.core.data.repository.AccountRepository
import com.dumbopanda.ledger.core.data.repository.HistoryRepository
import com.dumbopanda.ledger.core.domain.GroupOwedToMeUseCase
import com.dumbopanda.ledger.core.domain.OwedToMeItem
import com.dumbopanda.ledger.core.domain.formatMoney
import com.dumbopanda.ledger.core.model.Account
import com.dumbopanda.ledger.core.model.Entry
import com.dumbopanda.ledger.core.model.isLoanLeg
import com.dumbopanda.ledger.core.model.isOpeningBalance
import com.dumbopanda.ledger.core.model.TxnDirection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
import kotlin.math.roundToLong

enum class Breakdown(val label: String) { NAME("By name"), CATEGORY("By category"), ACCOUNT("By account") }

data class BalanceTile(val name: String, val minor: Long, val position: Int)

data class SpendGroup(val name: String, val totalMinor: Long, val count: Int)

data class StatsUiState(
    val month: YearMonth,
    val isCurrentMonth: Boolean,
    val hasEntries: Boolean,
    val balances: List<BalanceTile>,
    val totalSpendMinor: Long,
    val totalHaveMinor: Long,
    val breakdown: Breakdown,
    val showAllGroups: Boolean,
    // Every spend group for the chosen cut, largest first -- the screen shows 6 unless "all".
    val groups: List<SpendGroup>,
    val facts: List<Pair<String, String>>,
)

// "Show all" / "Show less" appears past this many groups.
const val BREAKDOWN_PREVIEW = 6

/**
 * Stats -- "worked out from the same rows the history list is drawn from, for the same month,"
 * so the two screens can never disagree. Balances are every credit less every debit up to the
 * end of the chosen month (a self transfer nets out across accounts); spending and income
 * leave self transfers out, exactly as the web's folded rows do.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    historyRepository: HistoryRepository,
    accountRepository: AccountRepository,
    private val groupOwedToMe: GroupOwedToMeUseCase,
    private val clock: LedgerClock,
) : ViewModel() {

    private fun today(): LocalDate = clock.now().atZone(ZoneId.systemDefault()).toLocalDate()

    private val month = MutableStateFlow(YearMonth.from(today()))
    private val breakdown = MutableStateFlow(Breakdown.NAME)
    private val showAll = MutableStateFlow(false)

    private val entries: StateFlow<List<Entry>> = month.flatMapLatest { historyRepository.entriesInMonth(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val balances: StateFlow<Map<String, Long>> = month.flatMapLatest { historyRepository.balancesUpTo(it.atEndOfMonth()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val accounts: StateFlow<List<Account>> = accountRepository.allAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val openItems: StateFlow<List<OwedToMeItem>> = historyRepository.owedToMeEntries()
        .map { groupOwedToMe.group(it).filter { item -> item.isOpen } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(build())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(entries, balances, accounts, openItems) { _, _, _, _ -> }.collect { _uiState.value = build() }
        }
        viewModelScope.launch {
            combine(breakdown, showAll) { _, _ -> }.collect { _uiState.value = build() }
        }
    }

    fun setMonthOffset(offset: Int) {
        val target = YearMonth.from(today()).plusMonths(offset.toLong())
        if (target == month.value) return
        month.value = target
        showAll.value = false
    }

    fun onBreakdownChosen(choice: Breakdown) {
        breakdown.value = choice
        showAll.value = false
    }

    fun onShowAllToggled() {
        showAll.value = !showAll.value
    }

    private fun build(): StatsUiState {
        val rows = entries.value
        val accs = accounts.value
        val bal = balances.value
        val byId = accs.associateBy { it.id }
        // Only real spending and income: not your own money moving (transfers), not a loan going
        // out or coming back, not an account's starting balance. Balances still count all three.
        val flows = rows.filter { it.transferGroup == null && !it.isLoanLeg && !it.isOpeningBalance }
        val spend = flows.filter { it.direction == TxnDirection.Debit }
        val income = flows.filter { it.direction == TxnDirection.Credit }
        val totalSpend = spend.sumOf { it.amount.minor }

        // Every live account gets a tile; an archived one only while it still holds money.
        val tiles = accs.mapIndexedNotNull { index, account ->
            val minor = bal[account.id] ?: 0L
            if (account.isArchived && minor == 0L) null else BalanceTile(account.name, minor, index)
        }

        val keyOf: (Entry) -> String = when (breakdown.value) {
            Breakdown.NAME -> { e -> e.expenseName.trim().ifEmpty { "Untitled" } }
            Breakdown.CATEGORY -> { e -> e.txnType.trim().ifEmpty { "No category" } }
            Breakdown.ACCOUNT -> { e -> byId[e.accountId]?.name ?: "Removed account" }
        }
        val groups = spend.groupBy(keyOf)
            .map { (name, list) -> SpendGroup(name, list.sumOf { it.amount.minor }, list.size) }
            .sortedByDescending { it.totalMinor }

        val m = month.value
        val isCurrent = m == YearMonth.from(today())
        val elapsed = if (isCurrent) today().dayOfMonth else m.lengthOfMonth()
        val perDayRupees = if (elapsed > 0) (totalSpend / 100.0 / elapsed).roundToLong() else 0L
        val biggest = spend.maxByOrNull { it.amount.minor }
        val spendDays = spend.map { it.entryDate }.toSet().size
        val owed = openItems.value.sumOf { it.amount.minor }

        return StatsUiState(
            month = m,
            isCurrentMonth = isCurrent,
            hasEntries = rows.isNotEmpty(),
            balances = tiles,
            totalSpendMinor = totalSpend,
            totalHaveMinor = bal.values.sum(),
            breakdown = breakdown.value,
            showAllGroups = showAll.value,
            groups = groups,
            facts = listOf(
                // A transfer is one thing that happened, not its two stored rows.
                "Entries" to rows.filterNot { it.isOpeningBalance }.distinctBy { it.transferGroup ?: it.id }.size.toString(),
                "Money in" to "₹${formatMoney(income.sumOf { it.amount.minor })}",
                "Biggest expense" to (biggest?.let { "${it.expenseName} · ₹${formatMoney(it.amount.minor)}" } ?: "None yet"),
                "Daily average" to "₹${formatMoney(perDayRupees * 100)} over $elapsed ${if (elapsed == 1) "day" else "days"}",
                "Days with spending" to "$spendDays of $elapsed",
                "Owed to me" to if (owed > 0) "₹${formatMoney(owed)}" else "Nothing open",
            ),
        )
    }
}
