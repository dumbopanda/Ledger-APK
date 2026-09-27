package com.dumbopanda.ledger.core.navigation

/**
 * §7.0's nav graph, as plain route strings -- every screen here takes plain lambda callbacks
 * (`onFinished`, `onSignedIn`, ...) rather than a `NavController`, matching every existing
 * `XxxRoute` composable in this codebase (`SettingsRoute.onSignedOut`, `HistoryRoute
 * .onEditRequested`, ...), so these constants exist only for the one place that assembles the
 * graph (`LedgerNavHost`, app module) to route between them. No arguments are encoded in the
 * route strings themselves for the same reason -- `SignIn`'s "session expired" note and
 * `Home`'s are per-instance state the NavHost passes straight into the composable, not
 * something Nav-Compose needs to parse back out of a URL-shaped route.
 */
object LedgerDestinations {
    const val ONBOARDING = "onboarding"
    const val SIGN_IN = "sign_in"
    const val SIGN_IN_EXPIRED = "sign_in_expired"
    const val RESTORING = "restoring"
    const val PROFILE_SETUP = "profile_setup"
    const val APP_TOUR = "app_tour"
    // C15: Account setup is no longer its own destination -- see LedgerNavHost's HOME
    // composable, where it now renders as a popup over Home itself.
    const val FORCED_UPDATE = "forced_update"
    const val HOME = "home"
    const val HOME_SESSION_EXPIRED = "home_session_expired"
    const val DELETE_ACCOUNT = "delete_account"
}
