package com.dumbopanda.ledger.core.domain

import com.dumbopanda.ledger.core.model.TxnDirection
import com.dumbopanda.ledger.core.model.TxnId
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

// §7.5.9's validation order. Several cases here deliberately make MULTIPLE things wrong at
// once and assert on which single message wins -- the order is the point, not just that each
// rule works in isolation.
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

    private val shapeMessage = "A transfer is two rows and an entry is one — cancel and add a new one instead."

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
    fun `edit mode turning a transfer into a plain entry is blocked with the exact plan wording`() {
        val result = useCase.validate(draft(selfTransfer = false, editModeOriginalIsTransfer = true))
        assertEquals(EntryDraftValidation.Blocked(shapeMessage), result)
    }

    @Test
    fun `edit mode turning a plain entry into a transfer is blocked the same way`() {
        val result = useCase.validate(draft(selfTransfer = true, editModeOriginalIsTransfer = false))
        assertEquals(EntryDraftValidation.Blocked(shapeMessage), result)
    }

    @Test
    fun `a missing date is blocked`() {
        assertEquals(EntryDraftValidation.Blocked("Pick a date first"), useCase.validate(draft(resolvedDate = null)))
    }

    @Test
    fun `a blank expense name is blocked`() {
        assertEquals(EntryDraftValidation.Blocked("Add an expense name"), useCase.validate(draft(expenseName = "   ")))
    }

    @Test
    fun `an empty amount is blocked`() {
        assertEquals(
            EntryDraftValidation.Blocked("Add an amount"),
            useCase.validate(draft(amount = AmountFieldValue("", 0))),
        )
    }

    @Test
    fun `a zero amount is blocked, zero is not a value`() {
        assertEquals(
            EntryDraftValidation.Blocked("Add an amount"),
            useCase.validate(draft(amount = AmountFieldValue("0", 1))),
        )
    }

    // -- Order: the shape check must win over every field-level check ------------------------

    @Test
    fun `shape mismatch wins over a blank name -- the toggle-off is the real problem`() {
        // §7.5.9's own reasoning: turning transfer off also clears the name, so without this
        // ordering the user would see "Add an expense name" instead of the actual cause.
        val result = useCase.validate(
            draft(selfTransfer = false, editModeOriginalIsTransfer = true, expenseName = ""),
        )
        assertEquals(EntryDraftValidation.Blocked(shapeMessage), result)
    }

    @Test
    fun `shape mismatch wins over a missing date too`() {
        val result = useCase.validate(
            draft(selfTransfer = false, editModeOriginalIsTransfer = true, resolvedDate = null),
        )
        assertEquals(EntryDraftValidation.Blocked(shapeMessage), result)
    }

    @Test
    fun `a missing date wins over a blank name`() {
        val result = useCase.validate(draft(resolvedDate = null, expenseName = ""))
        assertEquals(EntryDraftValidation.Blocked("Pick a date first"), result)
    }

    @Test
    fun `a blank name wins over an invalid amount`() {
        val result = useCase.validate(draft(expenseName = "", amount = AmountFieldValue("", 0)))
        assertEquals(EntryDraftValidation.Blocked("Add an expense name"), result)
    }
}
