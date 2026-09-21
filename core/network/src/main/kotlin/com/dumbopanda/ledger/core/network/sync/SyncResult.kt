package com.dumbopanda.ledger.core.network.sync

// §5.3's four HTTP outcome classes, generalized across push and pull.
sealed interface SyncResult<out T> {
    data class Success<T>(val value: T) : SyncResult<T>
    data class ClientError(val message: String) : SyncResult<Nothing> // 4xx
    data object Unauthorized : SyncResult<Nothing> // 401
    data class ServerError(val message: String) : SyncResult<Nothing> // 5xx / IO
}
