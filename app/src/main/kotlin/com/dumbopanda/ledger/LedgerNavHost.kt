package com.dumbopanda.ledger

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dumbopanda.ledger.core.data.repository.BootRoute
import com.dumbopanda.ledger.core.navigation.LedgerDestinations
import com.dumbopanda.ledger.feature.auth.SignInRoute
import com.dumbopanda.ledger.feature.onboarding.OnboardingRoute
import com.dumbopanda.ledger.feature.settings.DeleteAccountRoute

/**
 * §7.0's nav graph, assembled the only place that can see every feature module. [bootRoute] is
 * [BootViewModel]'s already-resolved answer -- this NavHost never re-decides it, it only picks
 * the matching start destination and wires the edges out of it (§7.1's boot decision is a
 * one-shot value, not a node the graph revisits on back-press).
 */
@Composable
fun LedgerNavHost(bootRoute: BootRoute) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = bootRoute.toStartRoute()) {
        composable(LedgerDestinations.ONBOARDING) {
            OnboardingRoute(
                onFinished = {
                    navController.navigate(LedgerDestinations.SIGN_IN) {
                        popUpTo(LedgerDestinations.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        composable(LedgerDestinations.SIGN_IN) {
            SignInScreenAfterAuth(navController, sessionExpiredNote = false)
        }

        composable(LedgerDestinations.SIGN_IN_EXPIRED) {
            SignInScreenAfterAuth(navController, sessionExpiredNote = true)
        }

        composable(LedgerDestinations.RESTORING) {
            BiometricGate {
                RestoringRoute(
                    onDone = { navController.navigate(LedgerDestinations.ACCOUNT_SETUP) { popUpTo(0) } },
                    onContinueOffline = { navController.navigate(LedgerDestinations.ACCOUNT_SETUP) { popUpTo(0) } },
                )
            }
        }

        composable(LedgerDestinations.ACCOUNT_SETUP) {
            BiometricGate {
                AccountSetupRoute(
                    onDone = { navController.navigate(LedgerDestinations.HOME) { popUpTo(0) } },
                )
            }
        }

        composable(LedgerDestinations.HOME) {
            BiometricGate {
                HomeScreen(
                    sessionExpiredNote = false,
                    onSignedOut = { navController.navigateToSignInFresh() },
                    onDeleteAccountRequested = { navController.navigate(LedgerDestinations.DELETE_ACCOUNT) },
                )
            }
        }

        composable(LedgerDestinations.HOME_SESSION_EXPIRED) {
            BiometricGate {
                HomeScreen(
                    sessionExpiredNote = true,
                    onSignedOut = { navController.navigateToSignInFresh() },
                    onDeleteAccountRequested = { navController.navigate(LedgerDestinations.DELETE_ACCOUNT) },
                )
            }
        }

        composable(LedgerDestinations.DELETE_ACCOUNT) {
            DeleteAccountRoute(
                // §7.17 step 7: "Confirmation screen -> Onboarding." The account, session and
                // local database are all gone by this point (AccountDeletionRepository), so
                // this is the same end state as a genuinely fresh install -- popUpTo(0) means
                // there is no Home/Restoring left on the back stack to accidentally return to.
                onAccountDeleted = { navController.navigate(LedgerDestinations.ONBOARDING) { popUpTo(0) } },
                onCancel = { navController.popBackStack() },
            )
        }

        composable(LedgerDestinations.FORCED_UPDATE) {
            ForcedUpdateScreen()
        }
    }
}

@Composable
private fun SignInScreenAfterAuth(navController: NavHostController, sessionExpiredNote: Boolean) {
    val postSignInViewModel: PostSignInViewModel = hiltViewModel()
    SignInRoute(
        sessionExpiredNote = sessionExpiredNote,
        onSignedIn = {
            postSignInViewModel.onSignedIn()
            // A fresh Onboarding->SignIn has no local data by construction (never signed in
            // on this device before) -> Restoring. The expired-token recovery path already had
            // a populated database before it ever left Home, so it goes straight back there
            // instead of re-running a full restore it doesn't need.
            val destination = if (sessionExpiredNote) LedgerDestinations.HOME else LedgerDestinations.RESTORING
            navController.navigate(destination) { popUpTo(0) }
        },
    )
}

private fun NavHostController.navigateToSignInFresh() {
    // Deliberately SIGN_IN, not back through Onboarding: the person tapping "Sign out" already
    // sat through Onboarding this device's boot, or would not be at Home to begin with. §7.1
    // row 1 (no session -> Onboarding) governs the next COLD BOOT, not this in-session action.
    navigate(LedgerDestinations.SIGN_IN) { popUpTo(0) }
}

private fun BootRoute.toStartRoute(): String = when (this) {
    is BootRoute.Onboarding -> LedgerDestinations.ONBOARDING
    is BootRoute.SignIn -> if (sessionExpiredNote) LedgerDestinations.SIGN_IN_EXPIRED else LedgerDestinations.SIGN_IN
    is BootRoute.Restoring -> LedgerDestinations.RESTORING
    is BootRoute.Home -> if (sessionExpiredNote) LedgerDestinations.HOME_SESSION_EXPIRED else LedgerDestinations.HOME
    is BootRoute.ForcedUpdate -> LedgerDestinations.FORCED_UPDATE
}
