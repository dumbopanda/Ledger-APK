package com.dumbopanda.ledger.core.database.migration

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.dumbopanda.ledger.core.database.LedgerDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MigrationTest {
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        LedgerDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun `migrate 1 to 2 creates pending_ops and dead_letter with no data loss`() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO entries (id, clientUid, entryDate, txnId, accountId, expenseName, " +
                    "txnType, txnFreq, amountMinor, direction, createdAt, updatedAt) VALUES " +
                    "('e1', 'c1', 0, 'Nabc123', 'a1', 'Groceries', 'Food', 'one_off', 100, 'debit', 0, 0)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        migrated.query("SELECT COUNT(*) FROM entries").use { cursor ->
            cursor.moveToFirst()
            org.junit.Assert.assertEquals(1, cursor.getInt(0))
        }
        migrated.query("SELECT COUNT(*) FROM pending_ops").use { cursor ->
            cursor.moveToFirst()
            org.junit.Assert.assertEquals(0, cursor.getInt(0))
        }
        migrated.query("SELECT COUNT(*) FROM dead_letter").use { cursor ->
            cursor.moveToFirst()
            org.junit.Assert.assertEquals(0, cursor.getInt(0))
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
