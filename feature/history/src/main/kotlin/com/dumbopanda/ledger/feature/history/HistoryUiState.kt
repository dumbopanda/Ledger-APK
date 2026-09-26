package com.dumbopanda.ledger.feature.history

import com.dumbopanda.ledger.core.data.repository.EntrySyncMark
import com.dumbopanda.ledger.core.domain.DayGroup
import com.dumbopanda.ledger.core.domain.OwedToMeItem
import com.dumbopanda.ledger.core.model.Account
import com.dumbopanda.ledger.core.model.Entry
import java.time.LocalDate
import java.time.YearMonth

enum class HistoryTab { RECENT, OWED_TO_ME }

data class HistoryUiState(
    val tab: HistoryTab,
    // Every account, archived included -- an archived account's old entries still show.
    val accounts: Map<String, Account>,
    // Each account's position, which picks its tile/tag tone (see accountTone()).
    val accountOrder: Map<String, Int>,
    val month: YearMonth,
    val today: LocalDate,
    // The shown slice of the month (40 rows, then "Show earlier"), grouped by day.
    val dayGroups: List<DayGroup>,
    // Every row in the month -- Recent's count pill.
    val recentCount: Int,
    val hasMore: Boolean,
    val syncMarks: Map<String, EntrySyncMark>,
    val owedToMeOpen: List<OwedToMeItem>,
    val owedToMeClosed: List<OwedToMeItem>,
    val detail: EntryDetailState?,
)

/**
 * The entry-detail sheet (tap any row, Recent or Owed to me). [sibling] is the other leg of a
 * self transfer once it's resolved. [owedToMeItem] is set only for a Y id -- the Open/Close
 * leg toggle shows only when that item has both legs (`closeLegEntryId != null`).
 */
data class EntryDetailState(
    val entry: Entry,
    val sibling: Entry?,
    val owedToMeItem: OwedToMeItem?,
    val showingOpenLeg: Boolean,
    val showDeleteConfirm: Boolean,
    val deleting: Boolean,
)
