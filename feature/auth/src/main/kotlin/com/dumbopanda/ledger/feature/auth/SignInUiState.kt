package com.dumbopanda.ledger.feature.auth

// Exactly the state table in §7.3.
sealed interface SignInUiState {
    data object Idle : SignInUiState
    data object InProgress : SignInUiState
    data object NoGoogleAccountOnDevice : SignInUiState
    data class NetworkFailure(val message: String) : SignInUiState
    data object TokenRejected : SignInUiState
    data object Success : SignInUiState
}
