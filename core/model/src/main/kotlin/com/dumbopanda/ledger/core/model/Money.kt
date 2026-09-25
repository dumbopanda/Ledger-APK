package com.dumbopanda.ledger.core.model

@JvmInline
value class Money(val minor: Long) {
    init { require(minor > 0 && minor <= MAX_MINOR) }

    // "clean" (ungrouped) rupee text -- e.g. 450050L -> "4500.50", 450000L -> "4500". The
    // shape core:domain.FormatAmountUseCase.toDisplay expects, so callers that only have a
    // Money (not a live AmountFieldValue) can still reuse its en-IN grouping.
    fun toCleanRupeeString(): String {
        val rupees = minor / 100
        val paise = minor % 100
        return if (paise == 0L) rupees.toString() else "$rupees.${paise.toString().padStart(2, '0')}"
    }

    companion object { const val MAX_MINOR = 999_999_999L }
}
