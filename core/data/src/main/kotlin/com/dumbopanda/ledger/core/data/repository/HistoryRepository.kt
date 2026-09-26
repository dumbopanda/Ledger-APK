package com.dumbopanda.ledger.core.data.repository

import com.dumbopanda.ledger.core.data.mapper.toDomain
import com.dumbopanda.ledger.core.database.LedgerDatabase
import com.dumbopanda.ledger.core.model.Entry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val sessionDatabaseHolder: SessionDatabaseHolder,
) {
    // History's Recent tab and the Stats screen, for whichever month the shared toggle shows.
    // Day-grouping (and the 40-row "Show earlier" window) is the caller's job.
    fun entriesInMonth(month: YearMonth): Flow<List<Entry>> = entriesInMonth(month) { sessionDatabaseHolder.database() }

    // ui-spec.md's Owed to me: every live Y-leg, mapped to domain -- reactive and unwindowed
    // (the ledger's Y-ids are a small fraction of all entries). Grouping into open/closed rows
    // is the caller's job (`GroupOwedToMeUseCase`).
    fun owedToMeEntries(): Flow<List<Entry>> = owedToMeEntries { sessionDatabaseHolder.database() }

    // Add Payment's transaction-name dropdown, most-used first.
    fun expenseNamesByUse(): Flow<List<String>> =
        sessionDatabaseHolder.database().entryDao().observeNameUses().map { rows -> rows.map { it.name } }

    // Stats' balance tiles: accountId -> balance in minor units, as of [date] inclusive.
    fun balancesUpTo(date: LocalDate): Flow<Map<String, Long>> =
        sessionDatabaseHolder.database().entryDao().observeBalancesUpTo(date)
            .map { rows -> rows.associate { it.accountId to it.balanceMinor } }
}

// Top-level so tests can supply a fixed in-memory database directly, same reasoning as
// EntryRepository's write* functions.
internal fun entriesInMonth(month: YearMonth, databaseProvider: () -> LedgerDatabase): Flow<List<Entry>> =
    databaseProvider().entryDao().observeBetween(month.atDay(1), month.atEndOfMonth())
        .map { entities -> entities.map { it.toDomain() } }

internal fun owedToMeEntries(databaseProvider: () -> LedgerDatabase): Flow<List<Entry>> =
    databaseProvider().entryDao().liveYEntries().map { entities -> entities.map { it.toDomain() } }
