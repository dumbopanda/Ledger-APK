package com.dumbopanda.ledger.feature.entry.component

import com.dumbopanda.ledger.core.model.Account
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class AccountSwapTest {
    private fun account(id: String, name: String) = Account(id, "client-$id-uid", name, 0, null)

    private val domestic = account("1", "Domestic")
    private val international = account("2", "International")
    private val cash = account("3", "Cash")

    @Test
    fun `picking a non-colliding value on the From side only changes From`() {
        val (newFrom, newTo) = resolveAccountSwap(domestic, international, AccountSide.FROM, cash)
        assertEquals(cash, newFrom)
        assertEquals(international, newTo)
    }

    @Test
    fun `picking a non-colliding value on the To side only changes To`() {
        val (newFrom, newTo) = resolveAccountSwap(domestic, international, AccountSide.TO, cash)
        assertEquals(domestic, newFrom)
        assertEquals(cash, newTo)
    }

    @Test
    fun `tapping the To account's value on the From side swaps them in one tap`() {
        // From=Domestic, To=International, tap International on the From side.
        val (newFrom, newTo) = resolveAccountSwap(domestic, international, AccountSide.FROM, international)
        assertEquals(international, newFrom)
        assertEquals(domestic, newTo)
    }

    @Test
    fun `tapping the From account's value on the To side swaps them in one tap`() {
        val (newFrom, newTo) = resolveAccountSwap(domestic, international, AccountSide.TO, domestic)
        assertEquals(international, newFrom)
        assertEquals(domestic, newTo)
    }

    @Test
    fun `re-tapping the same side's own current value is a no-op`() {
        val (newFrom, newTo) = resolveAccountSwap(domestic, international, AccountSide.FROM, domestic)
        assertEquals(domestic, newFrom)
        assertEquals(international, newTo)
    }

    @Test
    fun `an unrelated account never appears out of nowhere on the other side`() {
        val (newFrom, newTo) = resolveAccountSwap(domestic, international, AccountSide.FROM, cash)
        assertEquals(setOf(cash, international), setOf(newFrom, newTo))
    }
}
