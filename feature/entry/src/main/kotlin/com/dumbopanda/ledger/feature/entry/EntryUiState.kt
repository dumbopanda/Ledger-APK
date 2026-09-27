package com.dumbopanda.ledger.feature.entry

import androidx.compose.ui.text.input.TextFieldValue
import com.dumbopanda.ledger.core.domain.DateFieldState
import com.dumbopanda.ledger.core.domain.DraftField
import com.dumbopanda.ledger.core.domain.EntryDraft
import com.dumbopanda.ledger.core.domain.OwedToMeItem
import com.dumbopanda.ledger.core.model.Account

/** `.quick-date-btn.active` -- which of Today/Yesterday (if either) describes the date field. */
enum class QuickDate { TODAY, YESTERDAY }

sealed interface EntryUiState {
    data object Loading : EntryUiState

    // Every account archived, or none set up -- the chips have nothing to show.
    data object NoAccounts : EntryUiState

    data class Ready(
        val draft: EntryDraft,
        val dateText: TextFieldValue,
        val dateFieldState: DateFieldState,
        val quickDate: QuickDate?,
        // The date defaulted to yesterday because it's before 5:30 am -- said out loud, since
        // otherwise it reads like a stale date (it did, in testing).
        val lateNightDefault: Boolean = false,
        val accounts: List<Account>,
        // Every name any entry has used, most-used first -- the Transaction name dropdown.
        val knownNames: List<String>,
        // "Add a new name…" was picked (or the name isn't one of [knownNames]): the dropdown
        // has become a text box, with its arrow still reopening the list.
        val nameCustomMode: Boolean,
        // Currently-open Y items only -- "Which open item".
        val openItems: List<OwedToMeItem>,
        // "Which one": false = New, true = Old.
        val backOld: Boolean,
        // The field a blocked save pointed at -- outlined red until it's edited.
        val invalid: DraftField?,
        // "Needs an amount, a name and a date" under the button; null once nothing's missing.
        val caption: String?,
        // "Add Entry" / "Add Entry • ₹450.00 Out" / "Save Changes • ₹5,000.00 Transfer".
        val ctaText: String,
        // Non-null while the "Add this entry?" sheet is open.
        val confirm: ConfirmSheetContent?,
        val saving: Boolean,
        val isEditing: Boolean,
        // The edit banner's second line: "Y3k9d2 · 14 Sept 2026 · ₹4,500".
        val editBannerSub: String?,
    ) : EntryUiState
}

/** One-shot things the screen reacts to once -- not state it re-shows on recomposition. */
sealed interface EntryEvent {
    data class Toast(val message: String, val durationMs: Long = 2200L) : EntryEvent

    // An edit was saved -- the web goes back to History so you can see it.
    data object EditSaved : EntryEvent

    // A blocked save puts the caret in the field that stopped it (the web's `el.focus()`).
    data class FocusField(val field: DraftField) : EntryEvent

    // A new entry was saved -- the form resets, so the keyboard has nothing left to type into.
    data object Saved : EntryEvent
}
