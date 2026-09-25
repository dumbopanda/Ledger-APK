package com.dumbopanda.ledger.core.data.backup

import com.dumbopanda.ledger.core.database.converter.EnumConverters
import com.dumbopanda.ledger.core.database.entity.AccountEntity
import com.dumbopanda.ledger.core.database.entity.EntryEntity
import java.time.Instant

private val enumConverters = EnumConverters()

fun AccountEntity.toExported(): ExportedAccount = ExportedAccount(
    id = id,
    clientUid = clientUid,
    name = name,
    sortOrder = sortOrder,
    archivedAt = archivedAt?.toString(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun ExportedAccount.toEntity(): AccountEntity = AccountEntity(
    id = id,
    clientUid = clientUid,
    name = name,
    sortOrder = sortOrder,
    archivedAt = archivedAt?.let(Instant::parse),
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
    deletedAt = null,
)

fun EntryEntity.toExported(): ExportedEntry = ExportedEntry(
    id = id,
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
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun ExportedEntry.toEntity(): EntryEntity = EntryEntity(
    id = id,
    clientUid = clientUid,
    entryDate = java.time.LocalDate.parse(entryDate),
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
    deletedAt = null,
)
