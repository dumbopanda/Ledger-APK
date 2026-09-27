package com.dumbopanda.ledger.core.domain

sealed interface AccountNameValidation {
    data object Valid : AccountNameValidation
    data class Invalid(val message: String) : AccountNameValidation
}

/**
 * §7.12's rename rule: "1–40 chars, case-insensitively unique among live accounts." Pure so
 * the UI can explain the problem before the user ever taps Save, per that section's own
 * words -- [otherLiveAccountNames] is every OTHER live account's name (the one being renamed
 * excluded by the caller), so renaming an account to the name it already has is never flagged
 * as a collision with itself.
 */
fun validateAccountName(name: String, otherLiveAccountNames: List<String>): AccountNameValidation {
    val trimmed = name.trim()
    if (trimmed.length !in 1..40) {
        return AccountNameValidation.Invalid("Keep it to 40 characters or fewer")
    }
    if (otherLiveAccountNames.any { it.equals(trimmed, ignoreCase = true) }) {
        return AccountNameValidation.Invalid("An account named \"$trimmed\" already exists")
    }
    return AccountNameValidation.Valid
}
