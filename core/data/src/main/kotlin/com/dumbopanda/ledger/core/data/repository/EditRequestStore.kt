package com.dumbopanda.ledger.core.data.repository

import com.dumbopanda.ledger.core.model.Entry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

// The entry being edited (and, for a transfer, its sibling leg) -- the tapped leg is already
// whichever one §7.10's Open/Close toggle currently selects, so [entry] alone is enough for a
// non-transfer edit.
data class EditRequest(val entry: Entry, val sibling: Entry?)

/**
 * §7.6's edit mode is reached from History's entry-detail sheet but lives on EntryScreen --
 * two different Hilt ViewModels (`HistoryViewModel`, `EntryViewModel`), with no navigation
 * graph yet to pass arguments through (§7.0's real nav is C9's job; today MainActivity just
 * switches which top-level screen composes). This `@Singleton` is the hand-off between them:
 * `HistoryViewModel` writes one [EditRequest], `EntryViewModel` observes it reactively (not
 * just once at init -- both ViewModels can outlive a single tab switch) and consumes it.
 */
@Singleton
class EditRequestStore @Inject constructor() {
    private val _request = MutableStateFlow<EditRequest?>(null)
    val request: StateFlow<EditRequest?> = _request.asStateFlow()

    fun request(entry: Entry, sibling: Entry?) {
        _request.value = EditRequest(entry, sibling)
    }

    fun consume() {
        _request.value = null
    }
}
