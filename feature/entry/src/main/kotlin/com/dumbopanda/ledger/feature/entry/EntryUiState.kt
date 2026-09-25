package com.dumbopanda.ledger.feature.entry

import androidx.compose.ui.text.input.TextFieldValue
import com.dumbopanda.ledger.core.domain.DateFieldState
import com.dumbopanda.ledger.core.domain.EntryDraft
import com.dumbopanda.ledger.core.domain.OwedToMeItem
import com.dumbopanda.ledger.core.model.Account

/** §7.5's Add Entry form, top to bottom per `ui-spec.md`'s reordered field list. */
sealed interface EntryUiState {
    data object Loading : EntryUiState

    // §7.12's "seed Domestic/International/Cash on first run" (C6) hasn't run yet, or every
    // account has been archived -- there's nothing for the account chips to show.
    data object NoAccounts : EntryUiState

    data class Ready(
        val draft: EntryDraft,
        val dateText: TextFieldValue,
        val dateFieldState: DateFieldState,
        val accounts: List<Account>,
        // Open items only -- this is §7.5.5's Old picker source, not the full Owed-to-me list.
        val openItems: List<OwedToMeItem>,
        // Null when the draft is valid -- the Save button's caption (§7.5.9).
        val blockedMessage: String?,
        val showConfirmSheet: Boolean,
        val confirmSentence: String,
        val saving: Boolean,
    ) : EntryUiState

    // Terminal for this VM instance -- the Route reacts to this to reset/navigate away.
    // A fresh EntryViewModel (e.g. re-entering the tab) starts a new Ready state from scratch.
    data object Saved : EntryUiState
}
