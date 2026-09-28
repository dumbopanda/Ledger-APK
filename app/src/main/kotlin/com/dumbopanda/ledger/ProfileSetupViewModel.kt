package com.dumbopanda.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dumbopanda.ledger.core.data.repository.AuthRepository
import com.dumbopanda.ledger.core.data.repository.ProfileDetailsFetch
import com.dumbopanda.ledger.core.data.repository.ProfileRepository
import com.dumbopanda.ledger.core.model.ProfileOccupations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProfileSetupUiState {
    data object Checking : ProfileSetupUiState

    // A returning user (second device, reinstall) already answered this -- forwards straight
    // past it, same shape AccountSetupUiState.AlreadyHasAccounts uses for the same reason.
    data object AlreadyHasProfile : ProfileSetupUiState

    data class Ready(
        val name: String,
        // False when the name already came from Google or the Sign up form -- the screen then
        // greets by it instead of asking again.
        val askName: Boolean = name.isBlank(),
        val age: String = "",
        val location: String = "",
        val occupation: String? = null,
        val nameError: String? = null,
        val ageError: String? = null,
        val saving: Boolean = false,
    ) : ProfileSetupUiState

    data object Done : ProfileSetupUiState
}

/**
 * C14: "tell us about yourself" -- runs once, between Restoring and Account setup, for every
 * new account regardless of sign-in method. A Google sign-in already carries a name (read via
 * [AuthRepository.nameHintFromProvider], Supabase's own GoTrue-stored ID token claim); an email
 * sign-in has nothing, so this closes that gap for both the same way rather than branching the
 * boot flow on which method was used.
 *
 * Only the name is ever required -- age/location/occupation stay optional even once this step
 * has run, matching how [ProfileRepository.saveProfileDetails] and the `profiles` table itself
 * treat them (nullable columns, 0012_profile_details.sql). If the save fails (offline, server
 * error), this still moves on to Account setup rather than blocking -- there's no offline queue
 * for this, deliberately: since [hasProfileDetails][com.dumbopanda.ledger.core.model.UserProfile
 * .hasProfileDetails] reads false again until the write actually lands, the very next cold boot
 * naturally re-asks, which doubles as this feature's retry mechanism without any extra code.
 */
@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ProfileSetupUiState>(ProfileSetupUiState.Checking)
    val uiState: StateFlow<ProfileSetupUiState> = _uiState.asStateFlow()

    val occupationOptions: List<String> = ProfileOccupations.OPTIONS

    init {
        viewModelScope.launch {
            _uiState.value = when (val fetch = profileRepository.fetchProfileDetails()) {
                is ProfileDetailsFetch.Found -> {
                    if (fetch.displayName != null) {
                        ProfileSetupUiState.AlreadyHasProfile
                    } else {
                        readyState(name = authRepository.nameHintFromProvider() ?: "")
                    }
                }
                // Not signed in shouldn't reach this screen at all (BiometricGate/NavHost only
                // route here post-sign-in); a failed fetch (offline, server hiccup) fails open
                // to "assume new" per the class doc's own retry-via-next-boot reasoning, same
                // as BootRepository's own documented "don't know, proceed" philosophy.
                ProfileDetailsFetch.NotSignedIn, ProfileDetailsFetch.Failed ->
                    readyState(name = authRepository.nameHintFromProvider() ?: "")
            }
        }
    }

    fun onNameChanged(name: String) = updateReady { it.copy(name = name, nameError = null) }
    fun onAgeChanged(age: String) = updateReady { it.copy(age = age.filter(Char::isDigit).take(3), ageError = null) }
    fun onLocationChanged(location: String) = updateReady { it.copy(location = location) }
    // A second tap on the selected chip clears it -- every answer here stays optional.
    fun onOccupationSelected(occupation: String) =
        updateReady { it.copy(occupation = if (it.occupation == occupation) null else occupation) }

    // "Skip for now": keeps only the name (still required), drops whatever optional answers
    // were half-filled.
    fun skip() {
        updateReady { it.copy(age = "", location = "", occupation = null, ageError = null) }
        confirm()
    }

    fun confirm() {
        val state = _uiState.value as? ProfileSetupUiState.Ready ?: return
        if (state.saving) return

        val trimmedName = state.name.trim()
        if (trimmedName.isEmpty()) {
            _uiState.value = state.copy(nameError = "Tell us what to call you")
            return
        }

        val age = state.age.toIntOrNull()
        // The Terms and the Play listing say 18 and over; the server allows 13-120, this is stricter.
        if (state.age.isNotBlank() && (age == null || age !in 18..120)) {
            _uiState.value = state.copy(ageError = if (age != null && age < 18) "Ledger is for people 18 and over." else "Enter your age in years, like 27.")
            return
        }

        _uiState.value = state.copy(saving = true)
        viewModelScope.launch {
            profileRepository.saveProfileDetails(
                displayName = trimmedName,
                age = age,
                location = state.location.trim().ifBlank { null },
                occupation = state.occupation,
            )
            _uiState.value = ProfileSetupUiState.Done
        }
    }

    private fun readyState(name: String) = ProfileSetupUiState.Ready(name = name)

    private inline fun updateReady(transform: (ProfileSetupUiState.Ready) -> ProfileSetupUiState.Ready) {
        val state = _uiState.value as? ProfileSetupUiState.Ready ?: return
        _uiState.value = transform(state)
    }
}
