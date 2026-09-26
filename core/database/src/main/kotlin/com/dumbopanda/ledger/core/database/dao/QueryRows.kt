package com.dumbopanda.ledger.core.database.dao

// Plain query-result shapes (not tables -- no schema or migration change). Column names match
// the SQL aliases in EntryDao / PendingOpDao exactly, which is how Room maps them.

/** One transaction name and how many live entries use it -- Add Payment's name list. */
data class NameUseRow(val name: String, val uses: Int)

/** Everything in less everything out for one account, up to some date -- Stats' balances. */
data class AccountBalanceRow(val accountId: String, val balanceMinor: Long)

/** One not-yet-pushed operation against an entry -- History's "on this device"/"edited" tags. */
data class PendingEntryOpRow(val clientUid: String, val op: String)
