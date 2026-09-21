package com.dumbopanda.ledger.core.data.mapper

import com.dumbopanda.ledger.core.database.entity.AccountEntity
import com.dumbopanda.ledger.core.network.dto.AccountDto
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
