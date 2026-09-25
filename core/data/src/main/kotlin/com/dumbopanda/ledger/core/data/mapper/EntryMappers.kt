package com.dumbopanda.ledger.core.data.mapper

import com.dumbopanda.ledger.core.database.converter.EnumConverters
import com.dumbopanda.ledger.core.database.entity.EntryEntity
import com.dumbopanda.ledger.core.model.Entry
import com.dumbopanda.ledger.core.model.Money
import com.dumbopanda.ledger.core.model.TxnId
import com.dumbopanda.ledger.core.network.dto.EntryDto
import com.dumbopanda.ledger.core.network.sync.EntryPushRow
import java.time.Instant
import java.time.LocalDate

private val enumConverters = EnumConverters()

// A pulled row's id is always the server's (§5.4) -- push_entries never
// accepts a client-supplied one, and Pusher reconciles a locally-created
// row's id to match after its first successful push, so this never creates
// a duplicate local row.
fun EntryDto.toEntity(): EntryEntity = EntryEntity(
    id = id,
    clientUid = clientUid,
    entryDate = LocalDate.parse(entryDate),
    txnId = txnId,
    accountId = accountId,
    expenseName = expenseName,
    moreInfo = moreInfo,
    txnType = txnType,
    txnFreq = enumConverters.toFrequency(txnFreq),
    amountMinor = amountMinor,
    direction = enumConverters.toDirection(direction),
    transferGroup = transferGroup,
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
    deletedAt = deletedAt?.let(Instant::parse),
)

// A freshly created OR edited local row. [updatedAt] defaults to [createdAt] -- right for a
// brand-new row (both are "now"), wrong for C4's edit path, which passes the real current
// time while [createdAt] stays whatever the caller froze at edit-start (§7.6: "the original
// rows are frozen at the moment editing starts"), so an edited entry doesn't jump to the top
// of its day in History just because it was touched.
fun Entry.toEntity(updatedAt: Instant = createdAt): EntryEntity = EntryEntity(
    id = id,
    clientUid = clientUid,
    entryDate = entryDate,
    txnId = txnId.value,
    accountId = accountId,
    expenseName = expenseName,
    moreInfo = moreInfo,
    txnType = txnType,
    txnFreq = txnFreq,
    amountMinor = amount.minor,
    direction = direction,
    transferGroup = transferGroup,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = null,
)

fun EntryEntity.toDomain(): Entry = Entry(
    id = id,
    clientUid = clientUid,
    entryDate = entryDate,
    txnId = TxnId(txnId),
    accountId = accountId,
    expenseName = expenseName,
    moreInfo = moreInfo,
    txnType = txnType,
    txnFreq = txnFreq,
    amount = Money(amountMinor),
    direction = direction,
    transferGroup = transferGroup,
    createdAt = createdAt,
)

// The wire shape (§5.1) -- also what `pending_ops.payload_json` stores, so this is
// called once, at enqueue time, never re-derived from the entity on every push.
fun EntryEntity.toPushRow(): EntryPushRow = EntryPushRow(
    clientUid = clientUid,
    entryDate = entryDate.toString(),
    txnId = txnId,
    accountId = accountId,
    expenseName = expenseName,
    moreInfo = moreInfo,
    txnType = txnType,
    txnFreq = enumConverters.fromFrequency(txnFreq),
    amountMinor = amountMinor,
    direction = enumConverters.fromDirection(direction),
    transferGroup = transferGroup,
    deletedAt = deletedAt?.toString(),
)
