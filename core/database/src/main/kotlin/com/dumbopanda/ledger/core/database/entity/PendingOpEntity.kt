package com.dumbopanda.ledger.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

// The op log (§5.1). seq is the total order: ops are pushed in seq order and
// the push stops at the first failure, so a later op can never overtake one
// it depends on (e.g. a transfer's second leg overtaking its first).
@Entity(
    tableName = "pending_ops",
    indices = [Index(value = ["entityClientUid"])],
)
data class PendingOpEntity(
    @PrimaryKey(autoGenerate = true) val seq: Long = 0,
    val entityType: String, // "entry" | "account"
    val entityClientUid: String,
    val op: String, // "insert" | "update" | "delete"
    val payloadJson: String,
    val createdAt: Instant,
    val attempts: Int = 0,
    val lastError: String? = null,
)
