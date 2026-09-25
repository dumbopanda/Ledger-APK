package com.dumbopanda.ledger.feature.entry

import com.dumbopanda.ledger.core.domain.AmountFieldValue
import com.dumbopanda.ledger.core.domain.FormatAmountUseCase
import com.dumbopanda.ledger.core.model.Account
import com.dumbopanda.ledger.core.model.TxnDirection
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SENTENCE_DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE d MMM yyyy", Locale.ENGLISH)

/**
 * §7.5.10's confirm-sheet sentence: "₹4,500.00 left your Domestic account on Monday 14 Sep
 * 2026 for Rapido." / a transfer's "₹10,000.00 moved from your Domestic account to your Cash
 * account on … Two rows, one thing that happened." Pure so it's testable without a sheet.
 * Callers only reach this once [amount] is positive and [date] is resolved -- the same
 * preconditions `ValidateEntryDraftUseCase` already enforces before the sheet can open.
 */
fun buildConfirmSentence(
    amount: AmountFieldValue,
    direction: TxnDirection,
    date: LocalDate,
    expenseName: String,
    account: Account,
    selfTransfer: Boolean,
    toAccount: Account,
): String {
    val formatted = "₹" + FormatAmountUseCase().toDisplay(amount.clean)
    val dateText = date.format(SENTENCE_DATE_FORMAT)
    return if (selfTransfer) {
        "$formatted moved from your ${account.name} account to your ${toAccount.name} account on $dateText."
    } else {
        val verb = if (direction == TxnDirection.Credit) "came into" else "left"
        "$formatted $verb your ${account.name} account on $dateText for $expenseName."
    }
}
