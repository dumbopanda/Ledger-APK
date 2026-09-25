package com.dumbopanda.ledger.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dumbopanda.ledger.core.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * §7.2 pane 3's "Agree and continue" -- only ever writes the device-local consent record
 * ([ProfileRepository.recordLocalConsent]). The server-side `profiles.consent_version` write
 * needs a signed-in `auth.uid()` that doesn't exist yet at this point in the boot flow (§7.1
 * row 1 puts Onboarding BEFORE SignIn); [ProfileRepository.syncConsentToServerIfNeeded] is
 * called after sign-in succeeds instead, not from here.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {
    fun agreeAndContinue(onFinished: () -> Unit) {
        viewModelScope.launch {
            profileRepository.recordLocalConsent()
            onFinished()
        }
    }
}
