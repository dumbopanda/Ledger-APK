package com.dumbopanda.ledger.feature.history

import com.dumbopanda.ledger.core.domain.FormatAmountUseCase
import com.dumbopanda.ledger.core.model.Money
import com.dumbopanda.ledger.core.model.TxnId
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DIALOG_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

private fun money(amount: Money): String = "₹" + FormatAmountUseCase().toDisplay(amount.toCleanRupeeString())

/**
 * §7.11's three confirm-dialog variants, in priority order (a transfer leg that also has an
 * open Y-id is still, first and foremost, "this deletes two rows" -- that's the more
 * consequential fact).
 */
fun buildDeleteConfirmMessage(
    amount: Money,
    expenseName: String,
    date: LocalDate,
    isTransferLeg: Boolean,
    fromAccountName: String? = null,
    toAccountName: String? = null,
    openTxnId: TxnId? = null,
): String = when {
    isTransferLeg -> "This is one half of a transfer. Deleting it deletes both rows. ${money(amount)} " +
        "${fromAccountName.orEmpty()} → ${toAccountName.orEmpty()}."
    openTxnId != null -> "This entry has an open ID (${openTxnId.value}). Deleting it removes it from Owed to me."
    else -> "Delete this entry? ${money(amount)} · $expenseName · ${date.format(DIALOG_DATE_FORMAT)}. This cannot be undone."
}
