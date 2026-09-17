package com.dumbopanda.ledger.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

// Mirrors supabase/migrations/0002_accounts.sql.
@Entity(
    tableName = "accounts",
    indices = [Index(value = ["clientUid"], unique = true)],
)
data class AccountEntity(
    @PrimaryKey val id: String,
    val clientUid: String,
    val name: String,
    val sortOrder: Int,
    val archivedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
)
