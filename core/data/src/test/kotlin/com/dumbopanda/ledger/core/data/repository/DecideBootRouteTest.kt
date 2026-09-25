package com.dumbopanda.ledger.core.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class DecideBootRouteTest {
    @Test
    fun `no stored session goes to onboarding`() {
        assertEquals(
            BootRoute.Onboarding,
            decideBootRoute(
                hasStoredSession = false,
                forcedUpdateRequired = false,
                isOnline = true,
                refreshSucceeded = false,
                hasLocalData = false,
            ),
        )
    }

    @Test
    fun `no stored session goes to onboarding even offline`() {
        assertEquals(
            BootRoute.Onboarding,
            decideBootRoute(
                hasStoredSession = false,
                forcedUpdateRequired = false,
                isOnline = false,
                refreshSucceeded = false,
                hasLocalData = false,
            ),
        )
    }

    @Test
    fun `session present, refresh fails, online goes to sign-in with the expired note`() {
        assertEquals(
            BootRoute.SignIn(sessionExpiredNote = true),
            decideBootRoute(
                hasStoredSession = true,
                forcedUpdateRequired = false,
                isOnline = true,
                refreshSucceeded = false,
                hasLocalData = true,
            ),
        )
    }

    @Test
    fun `the C9 gate -- airplane mode plus an expired token still opens the ledger`() {
        assertEquals(
            BootRoute.Home(sessionExpiredNote = true),
            decideBootRoute(
                hasStoredSession = true,
                forcedUpdateRequired = false,
                isOnline = false,
                refreshSucceeded = false,
                hasLocalData = true,
            ),
        )
    }

    @Test
    fun `offline with an expired token and an empty local database still goes home, not restoring`() {
        // Restoring requires a network pull; offline can never satisfy that, so the offline
        // row goes to Home regardless of hasLocalData -- an empty Home, not a login wall.
        assertEquals(
            BootRoute.Home(sessionExpiredNote = true),
            decideBootRoute(
                hasStoredSession = true,
                forcedUpdateRequired = false,
                isOnline = false,
                refreshSucceeded = false,
                hasLocalData = false,
            ),
        )
    }

    @Test
    fun `session valid, local database empty goes to restoring`() {
        assertEquals(
            BootRoute.Restoring,
            decideBootRoute(
                hasStoredSession = true,
                forcedUpdateRequired = false,
                isOnline = true,
                refreshSucceeded = true,
                hasLocalData = false,
            ),
        )
    }

    @Test
    fun `session valid, local database populated goes home`() {
        assertEquals(
            BootRoute.Home(),
            decideBootRoute(
                hasStoredSession = true,
                forcedUpdateRequired = false,
                isOnline = true,
                refreshSucceeded = true,
                hasLocalData = true,
            ),
        )
    }

    @Test
    fun `a forced update wins over every other row, including no session`() {
        assertEquals(
            BootRoute.ForcedUpdate,
            decideBootRoute(
                hasStoredSession = false,
                forcedUpdateRequired = true,
                isOnline = true,
                refreshSucceeded = false,
                hasLocalData = false,
            ),
        )
    }

    @Test
    fun `a forced update wins over a healthy signed-in session with local data`() {
        assertEquals(
            BootRoute.ForcedUpdate,
            decideBootRoute(
                hasStoredSession = true,
                forcedUpdateRequired = true,
                isOnline = true,
                refreshSucceeded = true,
                hasLocalData = true,
            ),
        )
    }
}
