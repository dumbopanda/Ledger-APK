package com.dumbopanda.ledger.core.model

import java.time.Instant

// App-wide sync status (§5.3): the push loop is Idle/Syncing, lands on Synced
// or Error, and a repeated 401 refresh failure surfaces as SessionExpired —
// which the UI must be able to show at Home even when offline (§7.1).
sealed interface SyncState {
    data object Idle : SyncState
    data object Syncing : SyncState
    data class Synced(val at: Instant) : SyncState
    data class Error(val message: String) : SyncState
    data object SessionExpired : SyncState
}
