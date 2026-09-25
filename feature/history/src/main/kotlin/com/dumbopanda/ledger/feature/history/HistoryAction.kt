package com.dumbopanda.ledger.feature.history

import com.dumbopanda.ledger.core.domain.OwedToMeItem
import com.dumbopanda.ledger.core.model.Entry

sealed interface HistoryAction {
    data class TabSelected(val tab: HistoryTab) : HistoryAction
    data class EntryTapped(val entry: Entry) : HistoryAction
    // §7.8: an open item opens its one leg; a closed item opens the close (repayment) leg by
    // default. The row only has an OwedToMeItem to tap, not a full Entry -- the ViewModel
    // resolves which leg from its own already-loaded owedToMeEntries.
    data class OwedItemTapped(val item: OwedToMeItem) : HistoryAction
    data object DetailDismissed : HistoryAction
    data class LegToggled(val showOpen: Boolean) : HistoryAction
    data object DeleteTapped : HistoryAction
    data object DeleteConfirmed : HistoryAction
    data object DeleteCancelled : HistoryAction
    // C4's job -- present so the 50/50 row has both buttons now, wired once edit mode exists.
    data object EditTapped : HistoryAction
}
