package com.dumbopanda.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dumbopanda.ledger.core.data.repository.ProfileRepository
import com.dumbopanda.ledger.core.data.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Everything that must happen once, right after a successful sign-in, before the NavHost moves
 * on to Restoring/Home -- and nowhere else calls either of these:
 *  - [ProfileRepository.syncConsentToServerIfNeeded]: Onboarding could only record consent
 *    locally (§7.2 runs before there's a session); this is the first point an `auth.uid()`
 *    exists to write `profiles.consent_version`/`consent_at` against.
 *  - [SyncScheduler.onSessionRestored]: §5.5's trigger 6, C7's own flagged gap ("needs a
 *    session-expiry recovery flow that doesn't exist yet") -- it does now. Harmless to call on
 *    a FRESH sign-in too (nothing is pending yet), so both the Onboarding->SignIn path and the
 *    boot-time SignIn(expired)->SignIn path call this the same way rather than threading a
 *    "was this a recovery" flag through just to skip a no-op enqueue.
 */
@HiltViewModel
class PostSignInViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {
    fun onSignedIn() {
        viewModelScope.launch {
            profileRepository.syncConsentToServerIfNeeded()
            syncScheduler.onSessionRestored()
        }
    }
}
