package com.dumbopanda.ledger.core.domain

import com.dumbopanda.ledger.core.model.Account
import com.dumbopanda.ledger.core.model.TxnDirection
import com.dumbopanda.ledger.core.model.TxnId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

class ApplySelfTransferToggleUseCaseTest {
    private val useCase = ApplySelfTransferToggleUseCase(GenerateTxnIdUseCase())

    private val domestic = Account("1", "client-1-uid", "Domestic", 0, null)
    private val international = Account("2", "client-2-uid", "International", 1, null)
    private val cash = Account("3", "client-3-uid", "Cash", 2, null)
    private val accounts = listOf(domestic, international, cash)

    private fun draft(
        expenseName: String = "Groceries",
        selfTransfer: Boolean = false,
        accountId: String = domestic.id,
        toAccountId: String = international.id,
        txnId: TxnId = TxnId("Nabc123"),
        direction: TxnDirection = TxnDirection.Debit,
    ) = EntryDraft(
        resolvedDate = LocalDate.of(2026, 9, 24),
        expenseName = expenseName,
        moreInfo = null,
        selfTransfer = selfTransfer,
        accountId = accountId,
        toAccountId = toAccountId,
        txnId = txnId,
        txnType = "Payment",
        sentLegType = "",
        receivedLegType = "",
        amount = AmountFieldValue("500", 3),
        direction = direction,
        editModeOriginalIsTransfer = null,
    )

    // -- Turning ON -----------------------------------------------------------------------

    @Test
    fun `turning on regenerates an N id to Y`() {
        val result = useCase.apply(draft(txnId = TxnId("Nabc123")), turningOn = true, accounts, Random(1))
        assertTrue(result.txnId.isY)
    }

    @Test
    fun `turning on with an already-Y id leaves that exact id alone`() {
        val existingY = TxnId("Yabc123")
        val result = useCase.apply(draft(txnId = existingY), turningOn = true, accounts, Random(1))
        assertEquals(existingY, result.txnId)
    }

    @Test
    fun `turning on auto-fills the expense name regardless of what was there`() {
        val result = useCase.apply(draft(expenseName = "Groceries"), turningOn = true, accounts, Random(1))
        assertEquals("Self Transfer", result.expenseName)
    }

    @Test
    fun `turning on sets both leg type labels`() {
        val result = useCase.apply(draft(), turningOn = true, accounts, Random(1))
        assertEquals("Sent Transfer", result.sentLegType)
        assertEquals("Received Transfer", result.receivedLegType)
    }

    @Test
    fun `turning on sets the self transfer flag`() {
        val result = useCase.apply(draft(selfTransfer = false), turningOn = true, accounts, Random(1))
        assertTrue(result.selfTransfer)
    }

    @Test
    fun `turning on with distinct From and To accounts leaves To untouched`() {
        val result = useCase.apply(
            draft(accountId = domestic.id, toAccountId = international.id),
            turningOn = true,
            accounts,
            Random(1),
        )
        assertEquals(international.id, result.toAccountId)
    }

    @Test
    fun `turning on with From and To colliding silently moves To to a different account`() {
        val result = useCase.apply(
            draft(accountId = domestic.id, toAccountId = domestic.id),
            turningOn = true,
            accounts,
            Random(1),
        )
        assertNotEquals(domestic.id, result.toAccountId)
        assertTrue(accounts.any { it.id == result.toAccountId })
    }

    @Test
    fun `turning on with only one account available leaves the collision as the least-bad option`() {
        val result = useCase.apply(
            draft(accountId = domestic.id, toAccountId = domestic.id),
            turningOn = true,
            accounts = listOf(domestic),
            random = Random(1),
        )
        // There's nowhere else to move To -- this is a real edge case (a brand-new user with
        // exactly one account can't self-transfer at all), documented here rather than crashing.
        assertEquals(domestic.id, result.toAccountId)
    }

    // -- Turning OFF ------------------------------------------------------------------------

    @Test
    fun `turning off regenerates a Y id to N`() {
        val result = useCase.apply(draft(txnId = TxnId("Yabc123")), turningOn = false, accounts, Random(1))
        assertTrue(!result.txnId.isY)
    }

    @Test
    fun `turning off with an already-N id leaves that exact id alone`() {
        val existingN = TxnId("Nabc123")
        val result = useCase.apply(draft(txnId = existingN), turningOn = false, accounts, Random(1))
        assertEquals(existingN, result.txnId)
    }

    @Test
    fun `turning off clears the name only when it is still exactly the auto-fill`() {
        val result = useCase.apply(draft(expenseName = "Self Transfer"), turningOn = false, accounts, Random(1))
        assertEquals("", result.expenseName)
    }

    @Test
    fun `turning off preserves a name the user typed over the auto-fill`() {
        val result = useCase.apply(draft(expenseName = "Cash to wallet"), turningOn = false, accounts, Random(1))
        assertEquals("Cash to wallet", result.expenseName)
    }

    @Test
    fun `turning off resets direction to Debit even if it was Credit`() {
        val result = useCase.apply(draft(direction = TxnDirection.Credit), turningOn = false, accounts, Random(1))
        assertEquals(TxnDirection.Debit, result.direction)
    }

    @Test
    fun `turning off clears the self transfer flag`() {
        val result = useCase.apply(draft(selfTransfer = true), turningOn = false, accounts, Random(1))
        assertTrue(!result.selfTransfer)
    }
}
