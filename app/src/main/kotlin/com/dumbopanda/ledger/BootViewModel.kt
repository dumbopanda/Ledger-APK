package com.dumbopanda.ledger

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dumbopanda.ledger.core.data.repository.BootRepository
import com.dumbopanda.ledger.core.data.repository.BootRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * §7.1: "Single decision point, no UI beyond the system splash." [route] starts null and
 * [MainActivity] keeps the native splash screen up (`SplashScreen.setKeepOnScreenCondition`)
 * for exactly as long as it stays that way -- there is deliberately no loading composable of
 * our own underneath it.
 */
@HiltViewModel
class BootViewModel @Inject constructor(
    private val bootRepository: BootRepository,
) : ViewModel() {
    var route by mutableStateOf<BootRoute?>(null)
        private set

    init {
        viewModelScope.launch {
            route = bootRepository.decide()
        }
    }
}
