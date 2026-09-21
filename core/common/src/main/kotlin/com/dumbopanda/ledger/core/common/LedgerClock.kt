package com.dumbopanda.ledger.core.common

import java.time.Instant

// Testable seam for `now()` (§5's op log timestamps, settle/ceiling timers).
// A fake in core:testing swaps this for a fixed or advancing Instant in tests.
fun interface LedgerClock {
    fun now(): Instant
}

object SystemLedgerClock : LedgerClock {
    override fun now(): Instant = Instant.now()
}
