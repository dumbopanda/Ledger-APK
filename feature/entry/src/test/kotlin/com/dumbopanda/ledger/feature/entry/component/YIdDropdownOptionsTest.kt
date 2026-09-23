package com.dumbopanda.ledger.feature.entry.component

import com.dumbopanda.ledger.core.model.TxnId
import org.junit.Assert.assertEquals
import org.junit.Test

class YIdDropdownOptionsTest {
    private val fresh = TxnId("Yfresh1")
    private val open1 = TxnId("Yopen01")
    private val open2 = TxnId("Yopen02")

    @Test
    fun `a fresh N current id, no open ids -- just the fresh suggestion`() {
        val result = buildYIdDropdownOptions(TxnId("Nabcdef"), openYIds = emptyList(), freshId = fresh)
        assertEquals(listOf(fresh), result)
    }

    @Test
    fun `fresh id first, then every open id, for a brand-new Y entry`() {
        val result = buildYIdDropdownOptions(fresh, openYIds = listOf(open1, open2), freshId = fresh)
        assertEquals(listOf(fresh, open1, open2), result)
    }

    @Test
    fun `editing an entry whose stored Y-id is no longer open inserts it at the very top`() {
        val stored = TxnId("Yclosed")
        val result = buildYIdDropdownOptions(stored, openYIds = listOf(open1, open2), freshId = fresh)
        assertEquals(listOf(stored, fresh, open1, open2), result)
    }

    @Test
    fun `editing an entry whose stored Y-id IS still open does not duplicate it`() {
        val result = buildYIdDropdownOptions(open1, openYIds = listOf(open1, open2), freshId = fresh)
        assertEquals(listOf(fresh, open1, open2), result)
    }

    @Test
    fun `an N current id in edit mode never gets inserted -- only Y-ids can be open`() {
        val result = buildYIdDropdownOptions(TxnId("Nabcdef"), openYIds = listOf(open1), freshId = fresh)
        assertEquals(listOf(fresh, open1), result)
    }
}
