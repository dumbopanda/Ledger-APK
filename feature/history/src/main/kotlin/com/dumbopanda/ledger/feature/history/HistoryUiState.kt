package com.dumbopanda.ledger.feature.history

import com.dumbopanda.ledger.core.domain.MonthSummary
import com.dumbopanda.ledger.core.domain.OwedToMeItem
import com.dumbopanda.ledger.core.model.Account
import com.dumbopanda.ledger.core.model.Entry

enum class HistoryTab { RECENT, OWED_TO_ME }

data class HistoryUiState(
    val tab: HistoryTab,
    // Keyed by id -- every row needs to resolve accountId -> Account to render.
    val accounts: Map<String, Account>,
    val monthSummary: MonthSummary,
    val owedToMeOpen: List<OwedToMeItem>,
    val owedToMeClosed: List<OwedToMeItem>,
    val detail: EntryDetailState?,
)

/**
 * §7.10's entry detail sheet. [owedToMeItem] is non-null only when [entry]'s `txn_id` is a
 * `Y`-id currently known to the ledger (open or closed) -- the Open/Close toggle (§7.10, added
 * 25 Sep 2026) only renders when it's ALSO closed (`owedToMeItem.closeLegEntryId != null`).
 * [toAccount] is non-null only when [entry] is a transfer leg.
 */
data class EntryDetailState(
    val entry: Entry,
    val account: Account?,
    val toAccount: Account?,
    val owedToMeItem: OwedToMeItem?,
    // Which side the Open/Close toggle currently shows -- meaningful only when
    // owedToMeItem?.closeLegEntryId != null; ignored (and the toggle hidden) otherwise.
    val showingOpenLeg: Boolean,
    val showDeleteConfirm: Boolean,
    val deleteMessage: String,
    val deleting: Boolean,
)
