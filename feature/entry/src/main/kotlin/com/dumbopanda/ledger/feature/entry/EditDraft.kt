package com.dumbopanda.ledger.feature.entry

import com.dumbopanda.ledger.core.domain.AmountFieldValue
import com.dumbopanda.ledger.core.domain.EntryDraft
import com.dumbopanda.ledger.core.model.Account
import com.dumbopanda.ledger.core.model.Entry
import com.dumbopanda.ledger.core.model.TxnDirection
import com.dumbopanda.ledger.feature.entry.component.TXN_TYPE_OPTIONS
import java.time.Instant

/**
 * §7.6: "the original rows are frozen at the moment editing starts." This is that freeze --
 * captured once, when the edit begins, and used unchanged at save time no matter how many
 * times the draft itself is edited before then. Save always overwrites exactly these rows (by
 * `id`), never re-derived by matching content.
 */
sealed interface EditingOriginal {
    data class Plain(val id: String, val clientUid: String, val createdAt: Instant) : EditingOriginal
    data class Transfer(
        val transferGroup: String,
        val debitId: String,
        val debitClientUid: String,
        val debitCreatedAt: Instant,
        val creditId: String,
        val creditClientUid: String,
        val creditCreatedAt: Instant,
    ) : EditingOriginal
}

data class EditDraftResult(val draft: EntryDraft, val original: EditingOriginal)

/**
 * Builds the edit-mode starting draft from whichever leg was tapped ([entry] -- already
 * whichever side §7.10's Open/Close toggle selects, for a settled Y-id) and, for a transfer,
 * its already-resolved [sibling] leg. [accounts] supplies a fallback `toAccountId` for a
 * plain (non-transfer) entry, which still needs one per [EntryDraft]'s own contract even
 * though self-transfer is off.
 */
fun buildEditDraft(entry: Entry, sibling: Entry?, accounts: List<Account>): EditDraftResult {
    val isTransfer = sibling != null
    val debit = if (sibling == null || entry.direction == TxnDirection.Debit) entry else sibling
    val credit = if (sibling == null || entry.direction == TxnDirection.Credit) entry else sibling

    val original: EditingOriginal = if (isTransfer) {
        EditingOriginal.Transfer(
            transferGroup = requireNotNull(entry.transferGroup) { "a transfer leg always has a transferGroup" },
            debitId = debit.id,
            debitClientUid = debit.clientUid,
            debitCreatedAt = debit.createdAt,
            creditId = credit.id,
            creditClientUid = credit.clientUid,
            creditCreatedAt = credit.createdAt,
        )
    } else {
        EditingOriginal.Plain(entry.id, entry.clientUid, entry.createdAt)
    }

    val toAccountId = if (isTransfer) {
        credit.accountId
    } else {
        accounts.map(Account::id).firstOrNull { it != debit.accountId } ?: debit.accountId
    }

    val amountClean = entry.amount.toCleanRupeeString()
    val draft = EntryDraft(
        resolvedDate = entry.entryDate,
        expenseName = entry.expenseName,
        moreInfo = entry.moreInfo,
        selfTransfer = isTransfer,
        accountId = debit.accountId,
        toAccountId = toAccountId,
        txnId = entry.txnId,
        txnType = if (isTransfer) TXN_TYPE_OPTIONS.first() else entry.txnType,
        sentLegType = if (isTransfer) debit.txnType else "",
        receivedLegType = if (isTransfer) credit.txnType else "",
        amount = AmountFieldValue(amountClean, amountClean.length),
        direction = entry.direction,
        editModeOriginalIsTransfer = isTransfer,
    )
    return EditDraftResult(draft, original)
}
