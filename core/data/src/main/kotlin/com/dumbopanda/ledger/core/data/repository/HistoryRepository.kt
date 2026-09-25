package com.dumbopanda.ledger.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.dumbopanda.ledger.core.data.mapper.toDomain
import com.dumbopanda.ledger.core.database.LedgerDatabase
import com.dumbopanda.ledger.core.model.Entry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

private const val PAGE_SIZE = 40 // §7.7: "40 rows, then 'Show earlier entries'."

@Singleton
class HistoryRepository @Inject constructor(
    private val sessionDatabaseHolder: SessionDatabaseHolder,
) {
    // §7.7 / C1: Room's own PagingSource instead of a slice of an in-memory list.
    fun pagedEntries(): Flow<PagingData<Entry>> = pagedEntries { sessionDatabaseHolder.database() }

    // ui-spec.md's Owed to me: every live Y-leg, mapped to domain -- reactive and unpaged
    // (the ledger's Y-ids are a small fraction of all entries, so no PagingSource here).
    // Grouping into open/closed rows is the caller's job (`GroupOwedToMeUseCase`), same split
    // as `pagedEntries()` leaves day-grouping to the caller.
    fun owedToMeEntries(): Flow<List<Entry>> = owedToMeEntries { sessionDatabaseHolder.database() }

    // §7.7's month summary card -- out, in (minor units), transfers excluded. Returns the raw
    // pair rather than core:domain's `MonthSummary` -- core:data doesn't depend on core:domain
    // (§6.5/§6.6's layering), so the wrap into that type is the caller's (ViewModel's) job,
    // same split as `pagedEntries()` leaving day-grouping to `GroupEntriesByDayUseCase`.
    fun monthOutIn(month: YearMonth): Flow<Pair<Long, Long>> = monthOutIn(month) { sessionDatabaseHolder.database() }
}

// Top-level so tests can supply a fixed in-memory database directly, same reasoning as
// EntryRepository's write* functions. The lambda (not a plain `db` parameter) matters for
// production: Paging3 calls `pagingSourceFactory` again on every invalidation, and re-resolving
// the CURRENT session's database each time (rather than one captured at Flow-creation time)
// is what keeps a long-lived collector correct if the signed-in session ever changes under it.
internal fun pagedEntries(databaseProvider: () -> LedgerDatabase): Flow<PagingData<Entry>> =
    Pager(
        config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
        pagingSourceFactory = { databaseProvider().entryDao().pagingSource() },
    ).flow.map { pagingData -> pagingData.map { it.toDomain() } }

internal fun owedToMeEntries(databaseProvider: () -> LedgerDatabase): Flow<List<Entry>> =
    databaseProvider().entryDao().liveYEntries().map { entities -> entities.map { it.toDomain() } }

internal fun monthOutIn(month: YearMonth, databaseProvider: () -> LedgerDatabase): Flow<Pair<Long, Long>> {
    val dao = databaseProvider().entryDao()
    val from = month.atDay(1)
    val to = month.atEndOfMonth()
    return combine(dao.outMinorBetween(from, to), dao.inMinorBetween(from, to)) { out, income -> out to income }
}
