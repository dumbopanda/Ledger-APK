package com.dumbopanda.ledger.core.model

@JvmInline
value class Money(val minor: Long) {
    init { require(minor > 0 && minor <= MAX_MINOR) }
    companion object { const val MAX_MINOR = 999_999_999L }
}
