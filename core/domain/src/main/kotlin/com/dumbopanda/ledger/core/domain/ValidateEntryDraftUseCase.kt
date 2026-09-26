package com.dumbopanda.ledger.core.domain

import javax.inject.Inject

/** The field a blocked save points at -- the one index.html outlines in red (`.is-invalid`). */
enum class DraftField { DATE, AMOUNT, NAME }

sealed interface EntryDraftValidation {
    data object Valid : EntryDraftValidation
    data class Blocked(val message: String, val field: DraftField? = null) : EntryDraftValidation
}

/**
 * index.html's submit handler, check for check and word for word: the shape lock (an edit can't
 * turn one row into two or back) comes first, then date, then amount, then name. Shape before
 * name is not arbitrary -- turning the transfer switch off also clears the name, so checking
 * shape first is what names the real problem.
 */
class ValidateEntryDraftUseCase @Inject constructor() {
    fun validate(draft: EntryDraft): EntryDraftValidation {
        val original = draft.editModeOriginalIsTransfer
        if (original != null && original != draft.selfTransfer) {
            return EntryDraftValidation.Blocked(
                if (original) {
                    "A transfer is two rows and an entry is one — cancel and add a new one instead"
                } else {
                    "An entry is one row and a transfer is two — cancel and add a new one instead"
                },
            )
        }
        if (draft.resolvedDate == null) {
            return EntryDraftValidation.Blocked("Pick a date first", DraftField.DATE)
        }
        if (!draft.amount.isPositive()) {
            return EntryDraftValidation.Blocked("Add an amount", DraftField.AMOUNT)
        }
        if (draft.expenseName.isBlank()) {
            return EntryDraftValidation.Blocked("Pick a transaction name", DraftField.NAME)
        }
        return EntryDraftValidation.Valid
    }

    /**
     * The caption under Add Entry: "Needs an amount, a name and a date" -- every missing piece
     * in that order, or null once nothing is.
     */
    fun missingCaption(draft: EntryDraft): String? {
        val missing = buildList {
            if (!draft.amount.isPositive()) add("an amount")
            if (draft.expenseName.isBlank()) add("a name")
            if (draft.resolvedDate == null) add("a date")
        }
        if (missing.isEmpty()) return null
        val list = if (missing.size == 1) missing[0] else missing.dropLast(1).joinToString(", ") + " and " + missing.last()
        return "Needs $list"
    }

    private fun AmountFieldValue.isPositive(): Boolean = clean.toDoubleOrNull()?.let { it > 0.0 } ?: false
}
