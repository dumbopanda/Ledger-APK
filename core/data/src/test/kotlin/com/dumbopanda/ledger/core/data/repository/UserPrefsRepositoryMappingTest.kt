package com.dumbopanda.ledger.core.data.repository

import com.dumbopanda.ledger.core.data.sync.SyncMode
import org.junit.Assert.assertEquals
import org.junit.Test
import com.dumbopanda.ledger.core.datastore.SyncMode as ProtoSyncMode

class UserPrefsRepositoryMappingTest {
    @Test
    fun `every domain SyncMode round-trips through the proto and back`() {
        for (mode in SyncMode.entries) {
            assertEquals(mode, mode.toProto().toDomain())
        }
    }

    @Test
    fun `an unset or unrecognized proto value defaults to SMART, never crashes`() {
        assertEquals(SyncMode.SMART, ProtoSyncMode.SYNC_MODE_UNSPECIFIED.toDomain())
        assertEquals(SyncMode.SMART, ProtoSyncMode.UNRECOGNIZED.toDomain())
    }

    @Test
    fun `each proto value maps to its named counterpart`() {
        assertEquals(SyncMode.SMART, ProtoSyncMode.SYNC_MODE_SMART.toDomain())
        assertEquals(SyncMode.INSTANT, ProtoSyncMode.SYNC_MODE_INSTANT.toDomain())
        assertEquals(SyncMode.MANUAL, ProtoSyncMode.SYNC_MODE_MANUAL.toDomain())
    }
}
