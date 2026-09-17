package com.dumbopanda.ledger.core.model

@JvmInline
value class TxnId(val value: String) {
    init { require(REGEX.matches(value)) }
    val isY get() = value.startsWith('Y')
    companion object { val REGEX = Regex("^[YN][a-z0-9]{6}$") }
}
