package com.dumbopanda.ledger

import androidx.lifecycle.ViewModel
import com.dumbopanda.ledger.core.data.repository.UserPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class BiometricGateViewModel @Inject constructor(
    userPrefsRepository: UserPrefsRepository,
) : ViewModel() {
    val requireBiometric: Flow<Boolean> = userPrefsRepository.requireBiometricToOpen()
}
