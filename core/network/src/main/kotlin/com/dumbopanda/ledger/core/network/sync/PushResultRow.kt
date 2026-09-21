package com.dumbopanda.ledger.core.network.sync

// Unifies push_entries' RETURNS TABLE shape and the accounts upsert's
// returned row. `wasInsert` lets the caller tell "already had it" apart
// from "request failed" on a retry (0006_rpc.sql's own reasoning).
data class PushResultRow(
    val clientUid: String,
    val id: String,
    val updatedAt: String,
    val wasInsert: Boolean,
)
