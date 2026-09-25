package com.dumbopanda.ledger.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AccountNameValidationTest {
    @Test
    fun `a normal name among no collisions is valid`() {
        assertEquals(AccountNameValidation.Valid, validateAccountName("Wallet", emptyList()))
    }

    @Test
    fun `an empty name is invalid`() {
        assertEquals(true, validateAccountName("", listOf("Cash")) is AccountNameValidation.Invalid)
    }

    @Test
    fun `a name that is only whitespace is invalid`() {
        assertEquals(true, validateAccountName("   ", listOf("Cash")) is AccountNameValidation.Invalid)
    }

    @Test
    fun `a 41 character name is invalid`() {
        val name = "a".repeat(41)
        assertEquals(true, validateAccountName(name, emptyList()) is AccountNameValidation.Invalid)
    }

    @Test
    fun `a 40 character name is valid`() {
        val name = "a".repeat(40)
        assertEquals(AccountNameValidation.Valid, validateAccountName(name, emptyList()))
    }

    @Test
    fun `a case-insensitive collision with another account is invalid`() {
        assertEquals(true, validateAccountName("cash", listOf("Cash")) is AccountNameValidation.Invalid)
    }

    @Test
    fun `leading and trailing whitespace is trimmed before checking length and collision`() {
        assertEquals(AccountNameValidation.Valid, validateAccountName("  Wallet  ", listOf("Cash")))
        assertEquals(true, validateAccountName("  Cash  ", listOf("Cash")) is AccountNameValidation.Invalid)
    }

    @Test
    fun `renaming an account to its own current name is not a collision -- caller excludes it`() {
        // the caller is expected to have already filtered the account being renamed out of
        // otherLiveAccountNames, so its own current name isn't passed in here at all
        assertEquals(AccountNameValidation.Valid, validateAccountName("Cash", emptyList()))
    }
}
