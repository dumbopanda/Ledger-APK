package com.dumbopanda.ledger.core.domain

import com.dumbopanda.ledger.core.model.TxnDirection
import com.dumbopanda.ledger.core.model.TxnFrequency
import com.dumbopanda.ledger.core.model.TxnId
import java.time.LocalDate

/**
 * The Add/Edit Entry form's in-progress state (§7.5) -- looser than [com.dumbopanda.ledger.core.model.Entry]
 * on purpose, since a draft can be empty or mid-typing where a persisted row can't
 * (`resolvedDate` can be null, `amount` can be zero/unparsed). [accountId]/[toAccountId] are
 * always both populated, even outside transfer mode: the To-account row is "always present in
 * the tree, never added or removed" (§7.5.4), so there's nothing to invent when the user
 * switches self-transfer on.
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
    val frequency: TxnFrequency,
    val amount: AmountFieldValue,
    val direction: TxnDirection,
    // null in add mode; in edit mode, whether the entry being edited was ORIGINALLY a
    // transfer -- §7.6's shape-lock check compares this against the current [selfTransfer].
    val editModeOriginalIsTransfer: Boolean?,
)
