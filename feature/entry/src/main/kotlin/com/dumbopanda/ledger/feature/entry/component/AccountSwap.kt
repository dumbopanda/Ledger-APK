package com.dumbopanda.ledger.feature.entry.component

import com.dumbopanda.ledger.core.model.Account

enum class AccountSide { FROM, TO }

/**
 * §7.5.4's swap rule: tapping a value on one side that would collide with the other side
 * moves the OTHER side to whatever this side just gave up, instead of blocking the tap or
 * asking the user to resolve the conflict first. A non-colliding tap just sets that side.
 */
fun resolveAccountSwap(
    from: Account,
    to: Account,
    changedSide: AccountSide,
    newValue: Account,
): Pair<Account, Account> = when (changedSide) {
    AccountSide.FROM -> {
        val newTo = if (newValue.id == to.id) from else to
        newValue to newTo
    }
    AccountSide.TO -> {
        val newFrom = if (newValue.id == from.id) to else from
        newFrom to newValue
    }
}
