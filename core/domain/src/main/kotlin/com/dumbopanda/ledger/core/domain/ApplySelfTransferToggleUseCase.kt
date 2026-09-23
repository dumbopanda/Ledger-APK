package com.dumbopanda.ledger.core.domain

import com.dumbopanda.ledger.core.model.Account
import com.dumbopanda.ledger.core.model.TxnDirection
import com.dumbopanda.ledger.core.model.TxnFrequency
import javax.inject.Inject
import kotlin.random.Random

private const val SELF_TRANSFER_NAME = "Self Transfer"

/**
 * §7.5.3's 9 (on) + 5 (off) state changes, as one atomic transition on [EntryDraft] rather
 * than 9 separate field writes a caller could apply out of order or partially. Everything
 * that's purely a UI presentation detail with no draft-state consequence -- the account
 * label text, which fields are visible, the Y/N segment's disabled/dimmed look, the amount
 * direction buttons rendering as locked -- is left to the composables to derive directly
 * from `draft.selfTransfer`, not duplicated here.
 */
class ApplySelfTransferToggleUseCase @Inject constructor(
    private val generateTxnId: GenerateTxnIdUseCase,
) {
    fun apply(
        draft: EntryDraft,
        turningOn: Boolean,
        accounts: List<Account>,
        random: Random = Random.Default,
    ): EntryDraft = if (turningOn) turnOn(draft, accounts, random) else turnOff(draft, random)

    private fun turnOn(draft: EntryDraft, accounts: List<Account>, random: Random): EntryDraft {
        // "Setting the kind regenerates" (§7.5.5) -- but locking ON an ALREADY-Y id isn't a
        // kind change, so it's left alone rather than throwing away a deliberately chosen one.
        val newTxnId = if (draft.txnId.isY) draft.txnId else generateTxnId.generate(isY = true, random = random)

        val newToAccountId = if (draft.toAccountId == draft.accountId) {
            accounts.map(Account::id).firstOrNull { it != draft.accountId } ?: draft.toAccountId
        } else {
            draft.toAccountId
        }

        return draft.copy(
            selfTransfer = true,
            txnId = newTxnId,
            expenseName = SELF_TRANSFER_NAME,
            frequency = TxnFrequency.Monthly,
            sentLegType = "Sent Transfer",
            receivedLegType = "Received Transfer",
            toAccountId = newToAccountId,
        )
    }

    private fun turnOff(draft: EntryDraft, random: Random): EntryDraft {
        val newTxnId = if (!draft.txnId.isY) draft.txnId else generateTxnId.generate(isY = false, random = random)
        // Only clear the name if it's still exactly the auto-fill -- a user who typed over
        // it while transfer mode was on keeps what they typed.
        val newExpenseName = if (draft.expenseName == SELF_TRANSFER_NAME) "" else draft.expenseName

        return draft.copy(
            selfTransfer = false,
            txnId = newTxnId,
            direction = TxnDirection.Debit,
            expenseName = newExpenseName,
            frequency = TxnFrequency.Monthly,
        )
    }
}
