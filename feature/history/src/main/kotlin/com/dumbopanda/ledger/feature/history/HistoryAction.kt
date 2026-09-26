package com.dumbopanda.ledger.feature.history

import com.dumbopanda.ledger.core.domain.OwedToMeItem
import com.dumbopanda.ledger.core.model.Entry

sealed interface HistoryAction {
    data class TabSelected(val tab: HistoryTab) : HistoryAction
    data object ShowEarlierTapped : HistoryAction
    data class EntryTapped(val entry: Entry) : HistoryAction
    // An open item opens its one leg; a closed item opens the close (repayment) leg by
    // default -- "the more likely one to need a fix".
    data class OwedItemTapped(val item: OwedToMeItem) : HistoryAction
    data object DetailDismissed : HistoryAction
    data class LegToggled(val showOpen: Boolean) : HistoryAction
    data object DeleteTapped : HistoryAction
    data object DeleteConfirmed : HistoryAction
    data object DeleteCancelled : HistoryAction
    data object EditTapped : HistoryAction
}
