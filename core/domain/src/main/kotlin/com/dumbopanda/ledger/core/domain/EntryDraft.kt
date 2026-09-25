package com.dumbopanda.ledger.core.domain

import com.dumbopanda.ledger.core.model.TxnDirection
import com.dumbopanda.ledger.core.model.TxnId
import java.time.LocalDate

/**
 * The Add/Edit Entry form's in-progress state (§7.5) -- looser than [com.dumbopanda.ledger.core.model.Entry]
 * on purpose, since a draft can be empty or mid-typing where a persisted row can't
 * (`resolvedDate` can be null, `amount` can be zero/unparsed). [accountId]/[toAccountId] are
 * always both populated, even outside transfer mode: the To-account row is "always present in
 * the tree, never added or removed" (§7.5.4), so there's nothing to invent when the user
 * switches self-transfer on.
 *
 * No `frequency` field. `ui-spec.md`'s field list dropped it from the form entirely (settled
 * 21 Sep 2026) -- [com.dumbopanda.ledger.core.model.Entry.txnFreq] still exists underneath
 * because the server column does, but every draft writes it as `Monthly` unconditionally at
 * the point it becomes an `Entry`, never asks the user, and nothing here needs to carry a
 * value nobody sets.
 */
data class EntryDraft(
    val resolvedDate: LocalDate?,
    val expenseName: String,
    val moreInfo: String?,
    val selfTransfer: Boolean,
    val accountId: String,
    val toAccountId: String,
    val txnId: TxnId,
    val txnType: String,
    val sentLegType: String,
    val receivedLegType: String,
    val amount: AmountFieldValue,
    val direction: TxnDirection,
    // null in add mode; in edit mode, whether the entry being edited was ORIGINALLY a
    // transfer -- §7.6's shape-lock check compares this against the current [selfTransfer].
    val editModeOriginalIsTransfer: Boolean?,
)
