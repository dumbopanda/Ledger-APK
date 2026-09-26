package com.dumbopanda.ledger.feature.entry

import com.dumbopanda.ledger.core.domain.EntryDraft
import com.dumbopanda.ledger.core.domain.FormatAmountUseCase
import com.dumbopanda.ledger.core.domain.cleanToMinor
import com.dumbopanda.ledger.core.domain.formatConfirmDate
import com.dumbopanda.ledger.core.domain.formatDisplayDate
import com.dumbopanda.ledger.core.domain.formatMoneyFixed
import com.dumbopanda.ledger.core.model.Account
import com.dumbopanda.ledger.core.model.TxnDirection

/** Everything the "Add this entry?" sheet shows -- built in one pure function so it's testable. */
data class ConfirmSheetContent(
    val title: String,
    val line: String,
    // `.sheet-note` -- only for amounts of a lakh or more.
    val note: String?,
    val rows: List<Pair<String, String?>>,
    val backLabel: String,
    val confirmLabel: String,
)

/**
 * index.html's `openConfirmSheet`, sentence for sentence. [backOld] is the "Which one" choice
 * ("Old — this is money coming back"), which only matters when the entry is a Y.
 */
fun buildConfirmSheet(
    draft: EntryDraft,
    account: Account,
    toAccount: Account,
    backOld: Boolean,
    isEditing: Boolean,
    txnTypeFor: (String) -> String = { it.ifBlank { "Custom" } },
): ConfirmSheetContent {
    val date = requireNotNull(draft.resolvedDate) { "the sheet only opens once a date resolves" }
    val minor = cleanToMinor(draft.amount.clean) ?: 0L
    val amt = formatMoneyFixed(minor)
    val whenText = formatDisplayDate(date)
    val whenFull = formatConfirmDate(date)

    val line: String
    val rows: List<Pair<String, String?>>
    if (draft.selfTransfer) {
        line = "₹$amt moves from your ${account.name} account to your ${toAccount.name} account on $whenText. " +
            "It is your own money changing hands, so it is recorded as two rows but counts as neither spending nor income."
        rows = listOf(
            "Amount" to "₹$amt moved",
            "From account" to account.name,
            "To account" to toAccount.name,
            "Sent leg category" to txnTypeFor(draft.sentLegType),
            "Received leg category" to txnTypeFor(draft.receivedLegType),
            "Transaction name" to draft.expenseName,
            "Note" to draft.moreInfo,
            "Date" to whenFull,
            "Transaction ID" to draft.txnId.value,
        )
    } else {
        val debit = draft.direction == TxnDirection.Debit
        val head = if (debit) {
            "₹$amt leaves your ${account.name} account on $whenText for ${draft.expenseName}. "
        } else {
            "₹$amt comes into your ${account.name} account on $whenText from ${draft.expenseName}. "
        }
        val tail = when {
            !draft.txnId.isY -> "It is a one-way payment, so nothing is expected back."
            backOld -> "It closes an open item, so it drops off Owed to me once it is saved."
            else -> "It is marked to be returned, so it stays under Owed to me until the other half is entered."
        }
        line = head + tail
        rows = listOf(
            "Amount" to "₹$amt ${if (debit) "out" else "in"}",
            "Account" to account.name,
            "Category" to txnTypeFor(draft.txnType),
            "Transaction name" to draft.expenseName,
            "Note" to draft.moreInfo,
            "Date" to whenFull,
            "Will I get this back" to when {
                !draft.txnId.isY -> "No"
                backOld -> "Yes · closing an open one"
                else -> "Yes · a new one"
            },
            "Transaction ID" to draft.txnId.value,
        )
    }

    val lakh = FormatAmountUseCase().lakhReadback(draft.amount.clean)
    return ConfirmSheetContent(
        title = if (isEditing) "Save these changes?" else "Add this entry?",
        line = line,
        note = lakh?.let { "That is $it — check the zeros" },
        rows = rows,
        backLabel = if (isEditing) "Keep editing" else "Edit entry",
        confirmLabel = if (isEditing) "Save changes" else "Add entry",
    )
}
