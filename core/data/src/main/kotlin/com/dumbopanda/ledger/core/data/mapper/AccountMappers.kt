package com.dumbopanda.ledger.core.data.mapper

import com.dumbopanda.ledger.core.database.entity.AccountEntity
import com.dumbopanda.ledger.core.model.Account
import com.dumbopanda.ledger.core.network.dto.AccountDto
import com.dumbopanda.ledger.core.network.sync.AccountPushRow
import java.time.Instant

fun AccountDto.toEntity(): AccountEntity = AccountEntity(
    id = id,
    clientUid = clientUid,
    name = name,
    sortOrder = sortOrder,
    archivedAt = archivedAt?.let(Instant::parse),
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
    deletedAt = deletedAt?.let(Instant::parse),
)

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    clientUid = clientUid,
    name = name,
    sortOrder = sortOrder,
    archivedAt = archivedAt,
)

// A freshly created local account -- same reasoning as Entry.toEntity: not yet pushed, so
// updatedAt is just its own creation time and deletedAt is always null.
fun Account.toEntity(createdAt: Instant): AccountEntity = AccountEntity(
    id = id,
    clientUid = clientUid,
    name = name,
    sortOrder = sortOrder,
    archivedAt = archivedAt,
    createdAt = createdAt,
    updatedAt = createdAt,
    deletedAt = null,
)

fun AccountEntity.toPushRow(): AccountPushRow = AccountPushRow(
    clientUid = clientUid,
    name = name,
    sortOrder = sortOrder,
    archivedAt = archivedAt?.toString(),
    deletedAt = deletedAt?.toString(),
)
