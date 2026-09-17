package com.dumbopanda.ledger.core.model

import java.time.Instant

data class Account(
    val id: String,
    val clientUid: String,
    val name: String,
    val sortOrder: Int,
    val archivedAt: Instant?,
) {
    init {
        require(clientUid.length in 8..64)
        val trimmed = name.trim()
        require(trimmed.length in 1..40)
    }

    val isArchived get() = archivedAt != null
}
