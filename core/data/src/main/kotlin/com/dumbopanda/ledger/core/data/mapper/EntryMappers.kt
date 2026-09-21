package com.dumbopanda.ledger.core.data.mapper

import com.dumbopanda.ledger.core.database.converter.EnumConverters
import com.dumbopanda.ledger.core.database.entity.EntryEntity
import com.dumbopanda.ledger.core.network.dto.EntryDto
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
