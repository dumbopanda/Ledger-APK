package com.dumbopanda.ledger.feature.entry.component

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TxnTypeComboTest {
    @Test
    fun `every fixed option is not custom`() {
        TXN_TYPE_OPTIONS.forEach { assertFalse(it, isCustomTxnType(it)) }
    }

    @Test
    fun `an arbitrary stored string is custom`() {
        assertTrue(isCustomTxnType("Diwali bonus"))
    }

    @Test
    fun `an unrecognized stored type round-trips as custom, matching the plan's own rule`() {
        // §7.5.6: "Loading an entry whose stored type is not in the list opens the custom
        // field pre-filled -- a user-typed type must round-trip." isCustomTxnType being a pure
        // function of the stored value IS that round-trip: there's no separate flag to lose.
        val storedFromAnOlderEntry = "Grocery run"
        assertTrue(isCustomTxnType(storedFromAnOlderEntry))
    }

    @Test
    fun `the literal Custom menu label itself is not mistaken for a stored value`() {
        assertTrue(isCustomTxnType(TXN_TYPE_CUSTOM))
    }
}
