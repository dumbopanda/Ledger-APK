package com.dumbopanda.ledger.feature.entry.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dumbopanda.ledger.core.designsystem.LedgerTheme
import com.dumbopanda.ledger.core.designsystem.component.LedgerChip
import com.dumbopanda.ledger.core.model.Account

/**
 * §7.5.4. One scrollable row of every live (non-archived) account. Never disabled -- even the
 * side that would trigger a swap stays tappable, since [resolveAccountSwap] handles the
 * collision rather than the UI blocking it. Used once for a plain single-account picker, or
 * twice (From/To) by [TransferAccountChips] for the swap rule.
 */
@Composable
fun AccountChips(
    accounts: List<Account>,
    selected: Account,
    onSelect: (Account) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    Column(modifier = modifier) {
        if (label != null) {
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(top = 4.dp),
        ) {
            accounts.filterNot { it.isArchived }.forEach { account ->
                LedgerChip(
                    text = account.name,
                    selected = account.id == selected.id,
                    onClick = { onSelect(account) },
                )
            }
        }
    }
}

/** The two-row From/To picker, wiring both rows together through the swap rule. */
@Composable
fun TransferAccountChips(
    accounts: List<Account>,
    from: Account,
    to: Account,
    onChange: (from: Account, to: Account) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        AccountChips(
            accounts = accounts,
            selected = from,
            onSelect = { tapped ->
                val (newFrom, newTo) = resolveAccountSwap(from, to, AccountSide.FROM, tapped)
                onChange(newFrom, newTo)
            },
            label = "From account",
        )
        AccountChips(
            accounts = accounts,
            selected = to,
            onSelect = { tapped ->
                val (newFrom, newTo) = resolveAccountSwap(from, to, AccountSide.TO, tapped)
                onChange(newFrom, newTo)
            },
            label = "To account",
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Preview(fontScale = 1.0f)
@Preview(fontScale = 2.0f)
@Composable
private fun AccountChipsPreview() {
    val accounts = listOf(
        Account("1", "client-1-uid", "Domestic", 0, null),
        Account("2", "client-2-uid", "International", 1, null),
        Account("3", "client-3-uid", "Cash", 2, null),
    )
    LedgerTheme {
        AccountChips(
            accounts = accounts,
            selected = accounts[0],
            onSelect = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(fontScale = 1.0f)
@Composable
private fun TransferAccountChipsPreview() {
    val accounts = listOf(
        Account("1", "client-1-uid", "Domestic", 0, null),
        Account("2", "client-2-uid", "International", 1, null),
        Account("3", "client-3-uid", "Cash", 2, null),
    )
    LedgerTheme {
        TransferAccountChips(
            accounts = accounts,
            from = accounts[0],
            to = accounts[1],
            onChange = { _, _ -> },
            modifier = Modifier.padding(16.dp),
        )
    }
}
