package com.dumbopanda.ledger.core.data.repository

import android.util.Log
import com.dumbopanda.ledger.core.common.LedgerClock
import com.dumbopanda.ledger.core.datastore.LedgerDataStoreFactory
import com.dumbopanda.ledger.core.datastore.SessionStore
import com.dumbopanda.ledger.core.datastore.copy
import com.dumbopanda.ledger.core.network.dto.ProfileConsentUpdateDto
import com.dumbopanda.ledger.core.network.dto.ProfileDetailsFetchDto
import com.dumbopanda.ledger.core.network.dto.ProfileDetailsUpdateDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

// C14's own outcome shape, mirroring AccountDeletionOutcome/SyncOutcome's own file-local
// sealed-type convention rather than a shared generic result -- see those types' own comments.
sealed interface ProfileDetailsFetch {
    data class Found(val displayName: String?, val age: Int?, val location: String?, val occupation: String?) : ProfileDetailsFetch
    data object NotSignedIn : ProfileDetailsFetch
    data object Failed : ProfileDetailsFetch
}

// §7.2 pane 3 / §11.4. Starts at 1, not 0: 0 is ConsentState's own proto3 "never accepted"
// default, and a version that collides with "unset" would make a genuine v1 policy
// indistinguishable from nobody ever having agreed to anything. Bump this to force every
// device -- even ones that already agreed -- through pane 3 again on their next Onboarding.
private const val CURRENT_CONSENT_VERSION = 1

/**
 * §7.2's consent notice and §7.1's Onboarding-before-sign-in ordering, reconciled: the "Agree
 * and continue" tap happens with no session yet, so [recordLocalConsent] only ever writes the
 * device-scoped `ConsentState` DataStore (works offline, matches D9's "no login wall on a
 * plane" spirit extended to onboarding itself). `profiles.consent_version`/`consent_at` --
 * the versions §11.4 actually requires for a DPDP paper trail -- can only be written once
 * there IS a signed-in `auth.uid()` for RLS to check against, so [syncConsentToServerIfNeeded]
 * is called after sign-in succeeds and best-effort retries on every later successful sign-in
 * until it lands; a missed write here delays the compliance timestamp, it never blocks the UI
 * or loses the user's already-recorded local consent.
 */
@Singleton
class ProfileRepository @Inject constructor(
    private val dataStoreFactory: LedgerDataStoreFactory,
    private val sessionStore: SessionStore,
    private val supabase: SupabaseClient,
    private val clock: LedgerClock,
) {
    /** True once this device has agreed to the notice at [CURRENT_CONSENT_VERSION] or later. */
    suspend fun hasAcceptedCurrentConsent(): Boolean =
        dataStoreFactory.consentState().data.first().consentVersionAccepted >= CURRENT_CONSENT_VERSION

    suspend fun recordLocalConsent() {
        dataStoreFactory.consentState().updateData { current ->
            current.copy {
                consentVersionAccepted = CURRENT_CONSENT_VERSION
                consentAcceptedAtEpochMillis = clock.now().toEpochMilli()
            }
        }
    }

    /**
     * Best-effort; never throws. Called right after a successful sign-in (fresh from
     * Onboarding, or session-restored after §7.15's SignIn-again). If the local consent state
     * says nothing was ever accepted on this device, there is nothing to sync yet -- a user who
     * signed in on a second device before this one shows Onboarding here too, per §7.1 row 1.
     */
    suspend fun syncConsentToServerIfNeeded() {
        val state = dataStoreFactory.consentState().data.first()
        if (state.consentVersionAccepted <= 0) return
        val userId = sessionStore.read()?.userId ?: return

        runCatching {
            val acceptedAt = Instant.ofEpochMilli(state.consentAcceptedAtEpochMillis).toString()
            supabase.postgrest.from("profiles").update(
                ProfileConsentUpdateDto(
                    consentVersion = state.consentVersionAccepted,
                    consentAt = acceptedAt,
                    onboardedAt = acceptedAt,
                ),
            ) {
                filter { eq("user_id", userId) }
            }
        }.onFailure { e -> Log.w(TAG, "syncConsentToServerIfNeeded failed, will retry next sign-in", e) }
    }

    // C14: ProfileSetupViewModel's own "Checking" state -- whether this account has already
    // been through the "tell us about yourself" step, same "ask the server, don't guess from
    // local state" shape AccountSetupViewModel's hasAnyAccount() check uses for the same reason
    // (a second device, or a reinstall, must not re-ask someone who already answered).
    suspend fun fetchProfileDetails(): ProfileDetailsFetch {
        val userId = sessionStore.read()?.userId ?: return ProfileDetailsFetch.NotSignedIn
        return runCatching {
            val dto = supabase.postgrest.from("profiles")
                .select { filter { eq("user_id", userId) } }
                .decodeSingle<ProfileDetailsFetchDto>()
            ProfileDetailsFetch.Found(dto.displayName, dto.age, dto.location, dto.occupation)
        }.getOrElse { e -> Log.w(TAG, "fetchProfileDetails failed", e); ProfileDetailsFetch.Failed }
    }

    suspend fun saveProfileDetails(displayName: String, age: Int?, location: String?, occupation: String?): Result<Unit> {
        val userId = sessionStore.read()?.userId ?: return Result.failure(IllegalStateException("Not signed in"))
        return runCatching {
            supabase.postgrest.from("profiles").update(
                ProfileDetailsUpdateDto(displayName = displayName, age = age, location = location, occupation = occupation),
            ) {
                filter { eq("user_id", userId) }
            }
            Unit
        }.onFailure { e -> Log.e(TAG, "saveProfileDetails failed", e) }
    }

    private companion object {
        const val TAG = "ProfileRepository"
    }
}
