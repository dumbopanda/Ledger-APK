package com.dumbopanda.ledger.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dumbopanda.ledger.core.model.TxnDirection
import com.dumbopanda.ledger.core.model.TxnFrequency
import java.time.Instant
import java.time.LocalDate

// Mirrors supabase/migrations/0003_entries.sql. No userId column — isolation is
// the per-user database FILE (§6.4), not a row filter, unlike the server table.
@Entity(
    tableName = "entries",
    indices = [
        Index(value = ["clientUid"], unique = true),
        Index(value = ["entryDate", "createdAt"]),
        Index(value = ["transferGroup"]),
        Index(value = ["txnId"]),
    ],
)
data class EntryEntity(
    @PrimaryKey val id: String,
    val clientUid: String,
    val entryDate: LocalDate,
    val txnId: String,
    val accountId: String,
    val expenseName: String,
    val moreInfo: String?,
    val txnType: String,
    val txnFreq: TxnFrequency,
    val amountMinor: Long,
    val direction: TxnDirection,
    val transferGroup: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
)
