package com.dumbopanda.ledger.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

// §5.3 rule 4: a 4xx-rejected op is moved here, surfaced in Settings (C7),
// and never retried — a malformed op retried forever is a hot loop.
@Entity(tableName = "dead_letter")
data class DeadLetterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String,
    val entityClientUid: String,
    val op: String,
    val payloadJson: String,
    val error: String,
    val failedAt: Instant,
)
