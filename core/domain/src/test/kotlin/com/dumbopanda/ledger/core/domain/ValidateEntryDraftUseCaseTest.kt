package com.dumbopanda.ledger.core.domain

import com.dumbopanda.ledger.core.model.TxnDirection
import com.dumbopanda.ledger.core.model.TxnId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

// index.html's submit-time checks, in its order: shape, date, amount, name. Several cases make
// MULTIPLE things wrong at once and assert on which single message wins -- the order is the
// point, not just that each rule works in isolation.
class ValidateEntryDraftUseCaseTest {
    private val useCase = ValidateEntryDraftUseCase()

    private fun draft(
        resolvedDate: LocalDate? = LocalDate.of(2026, 9, 24),
        expenseName: String = "Groceries",
        amount: AmountFieldValue = AmountFieldValue("500", 3),
        selfTransfer: Boolean = false,
        editModeOriginalIsTransfer: Boolean? = null,
    ) = EntryDraft(
        resolvedDate = resolvedDate,
        expenseName = expenseName,
        moreInfo = null,
        selfTransfer = selfTransfer,
        accountId = "acc-1",
        toAccountId = "acc-2",
        txnId = TxnId("Nabc123"),
        txnType = "Payment",
        sentLegType = "",
        receivedLegType = "",
        amount = amount,
        direction = TxnDirection.Debit,
        editModeOriginalIsTransfer = editModeOriginalIsTransfer,
    )

    private val transferToEntry = "A transfer is two rows and an entry is one — cancel and add a new one instead"
    private val entryToTransfer = "An entry is one row and a transfer is two — cancel and add a new one instead"

    @Test
    fun `a fully valid draft passes`() {
        assertEquals(EntryDraftValidation.Valid, useCase.validate(draft()))
    }

    @Test
    fun `add mode never checks shape, regardless of the self-transfer flag`() {
        assertEquals(EntryDraftValidation.Valid, useCase.validate(draft(selfTransfer = true, editModeOriginalIsTransfer = null)))
    }

    @Test
    fun `edit mode with an unchanged shape is not blocked`() {
        assertEquals(EntryDraftValidation.Valid, useCase.validate(draft(selfTransfer = true, editModeOriginalIsTransfer = true)))
        assertEquals(EntryDraftValidation.Valid, useCase.validate(draft(selfTransfer = false, editModeOriginalIsTransfer = false)))
    }

    @Test
    fun `edit mode turning a transfer into a plain entry is blocked with the web's wording`() {
        val result = useCase.validate(draft(selfTransfer = false, editModeOriginalIsTransfer = true))
        assertEquals(EntryDraftValidation.Blocked(transferToEntry), result)
    }

    @Test
    fun `edit mode turning a plain entry into a transfer is blocked with the mirrored wording`() {
        val result = useCase.validate(draft(selfTransfer = true, editModeOriginalIsTransfer = false))
        assertEquals(EntryDraftValidation.Blocked(entryToTransfer), result)
    }

    @Test
    fun `a missing date is blocked and points at the date field`() {
        assertEquals(EntryDraftValidation.Blocked("Pick a date first", DraftField.DATE), useCase.validate(draft(resolvedDate = null)))
    }

    @Test
    fun `a blank name is blocked and points at the name field`() {
        assertEquals(
            EntryDraftValidation.Blocked("Pick a transaction name", DraftField.NAME),
            useCase.validate(draft(expenseName = "   ")),
        )
    }

    @Test
    fun `an empty amount is blocked`() {
        assertEquals(
            EntryDraftValidation.Blocked("Add an amount", DraftField.AMOUNT),
            useCase.validate(draft(amount = AmountFieldValue("", 0))),
        )
    }

    @Test
    fun `a zero amount is blocked, zero is not a value`() {
        assertEquals(
            EntryDraftValidation.Blocked("Add an amount", DraftField.AMOUNT),
            useCase.validate(draft(amount = AmountFieldValue("0", 1))),
        )
    }

    // -- Order ---------------------------------------------------------------------------------

    @Test
    fun `shape mismatch wins over a blank name -- the toggle-off is the real problem`() {
        val result = useCase.validate(draft(selfTransfer = false, editModeOriginalIsTransfer = true, expenseName = ""))
        assertEquals(EntryDraftValidation.Blocked(transferToEntry), result)
    }

    @Test
    fun `shape mismatch wins over a missing date too`() {
        val result = useCase.validate(draft(selfTransfer = false, editModeOriginalIsTransfer = true, resolvedDate = null))
        assertEquals(EntryDraftValidation.Blocked(transferToEntry), result)
    }

    @Test
    fun `a missing date wins over a missing amount and name`() {
        val result = useCase.validate(draft(resolvedDate = null, expenseName = "", amount = AmountFieldValue("", 0)))
        assertEquals(EntryDraftValidation.Blocked("Pick a date first", DraftField.DATE), result)
    }

    @Test
    fun `a missing amount wins over a blank name`() {
        val result = useCase.validate(draft(expenseName = "", amount = AmountFieldValue("", 0)))
        assertEquals(EntryDraftValidation.Blocked("Add an amount", DraftField.AMOUNT), result)
    }

    // -- The caption under Add Entry -------------------------------------------------------------

    @Test
    fun `caption lists every missing piece in the web's order`() {
        assertEquals(
            "Needs an amount, a name and a date",
            useCase.missingCaption(draft(resolvedDate = null, expenseName = "", amount = AmountFieldValue("", 0))),
        )
        assertEquals("Needs an amount and a name", useCase.missingCaption(draft(expenseName = "", amount = AmountFieldValue("", 0))))
        assertEquals("Needs a date", useCase.missingCaption(draft(resolvedDate = null)))
    }

    @Test
    fun `caption is null once nothing is missing`() {
        assertNull(useCase.missingCaption(draft()))
    }
}
