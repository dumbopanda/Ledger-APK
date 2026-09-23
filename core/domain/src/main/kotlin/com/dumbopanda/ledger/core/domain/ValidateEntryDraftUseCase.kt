package com.dumbopanda.ledger.core.domain

import javax.inject.Inject

sealed interface EntryDraftValidation {
    data object Valid : EntryDraftValidation
    data class Blocked(val message: String) : EntryDraftValidation
}

/**
 * §7.5.9's validation order, exactly -- shape-mismatch before name-empty is not arbitrary:
 * turning the transfer switch off also clears the name field (§7.5.3), so checking shape
 * FIRST is what makes the reported message name the real problem instead of "Add an expense
 * name" for someone who actually just flipped a toggle.
 */
class ValidateEntryDraftUseCase @Inject constructor() {
    fun validate(draft: EntryDraft): EntryDraftValidation {
        if (draft.editModeOriginalIsTransfer != null && draft.editModeOriginalIsTransfer != draft.selfTransfer) {
            return EntryDraftValidation.Blocked(
                "A transfer is two rows and an entry is one — cancel and add a new one instead.",
            )
        }
        if (draft.resolvedDate == null) {
            return EntryDraftValidation.Blocked("Pick a date first")
        }
        if (draft.expenseName.isBlank()) {
            return EntryDraftValidation.Blocked("Add an expense name")
        }
        if (!draft.amount.isPositive()) {
            return EntryDraftValidation.Blocked("Add an amount")
        }
        return EntryDraftValidation.Valid
    }

    private fun AmountFieldValue.isPositive(): Boolean = clean.toDoubleOrNull()?.let { it > 0.0 } ?: false
}
