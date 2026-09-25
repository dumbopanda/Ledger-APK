package com.dumbopanda.ledger.core.data.repository

/**
 * §7.1's boot decision, as a value instead of a side effect -- see [decideBootRoute]. Lives here
 * rather than core:domain for the same reason [SyncOutcome]/[PullOutcome] do: core:domain
 * already depends on core:data (its use cases consume repositories), so a domain-layer type
 * used BY a repository would be a cycle the other way round.
 *
 * [SignIn] carries [SignIn.sessionExpiredNote] because the same screen is reached two ways:
 * fresh from Onboarding (no note) and from a boot-time refresh failure (§7.15's "Signed out.
 * Your entries are safe on this device." banner) -- [decideBootRoute] only ever produces the
 * second kind. [Home.sessionExpiredNote] is the offline counterpart: row 3 lands on Home
 * directly rather than SignIn, but the user still needs to know why (`LedgerBanner`'s own doc
 * comment already names this note: "session expired — showing local data").
 */
sealed interface BootRoute {
    data object Onboarding : BootRoute
    data class SignIn(val sessionExpiredNote: Boolean) : BootRoute
    data object Restoring : BootRoute
    data class Home(val sessionExpiredNote: Boolean = false) : BootRoute
    data object ForcedUpdate : BootRoute
}

/**
 * §7.1's six-row table, as one pure function instead of [BootRepository] doing IO and deciding
 * in the same breath. The six [BootRoute] outcomes below are exactly the table's six rows, in
 * the table's own order:
 *
 * 1. No stored session -> [BootRoute.Onboarding]
 * 2. Session, refresh fails, online -> [BootRoute.SignIn] with the expired note
 * 3. Session, refresh fails, offline -> [BootRoute.Home] -- "local data is fully readable
 *    offline without a valid token." This is the row the C9 gate exists to prove: airplane
 *    mode plus an expired token must still open the ledger, not show a login wall.
 * 4. Session valid, local DB empty -> [BootRoute.Restoring]
 * 5. Session valid, local DB populated -> [BootRoute.Home]
 * 6. `min_supported_build` exceeded -> [BootRoute.ForcedUpdate]
 *
 * Row 6 is checked first and wins outright, [forcedUpdateRequired] having already folded in
 * "and we could actually reach the server to know that" -- offline, the caller passes false
 * rather than leaving a stale build permanently blocked with no way to prove it's since been
 * updated, matching the same "don't invent a wall on a plane" reasoning as row 3.
 *
 * [refreshSucceeded] is meaningless when [hasStoredSession] is false (there is nothing to
 * refresh); callers pass `false` in that case and it is never read.
 */
fun decideBootRoute(
    hasStoredSession: Boolean,
    forcedUpdateRequired: Boolean,
    isOnline: Boolean,
    refreshSucceeded: Boolean,
    hasLocalData: Boolean,
): BootRoute {
    if (forcedUpdateRequired) return BootRoute.ForcedUpdate
    if (!hasStoredSession) return BootRoute.Onboarding
    if (!isOnline) return BootRoute.Home(sessionExpiredNote = true)
    if (!refreshSucceeded) return BootRoute.SignIn(sessionExpiredNote = true)
    return if (hasLocalData) BootRoute.Home() else BootRoute.Restoring
}
