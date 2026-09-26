package com.dumbopanda.ledger.feature.entry.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dumbopanda.ledger.core.designsystem.component.LedgerPill
import com.dumbopanda.ledger.core.designsystem.component.PillStyles
import com.dumbopanda.ledger.core.model.Account

private const val PER_ROW = 3

/**
 * `.account-block .chip-group` -- equal-width chips (`flex: 1`), 8px apart, 12/700. The web has
 * exactly three accounts in one row; with the APK's N accounts, up to three share a row exactly
 * like that, and a 4th onward starts a new row of the same widths.
 */
@Composable
fun AccountChipRows(
    accounts: List<Account>,
    selectedId: String,
    onSelect: (Account) -> Unit,
    modifier: Modifier = Modifier,
) {
    val live = accounts.filterNot { it.isArchived }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        live.chunked(PER_ROW).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { account ->
                    LedgerPill(
                        text = account.name,
                        selected = account.id == selectedId,
                        onClick = { onSelect(account) },
                        style = PillStyles.Compact,
                        modifier = Modifier.weight(1f),
                    )
                }
                // Only pad a short last row when there IS more than one row -- a lone row of
                // two keeps the web's two-halves look.
                if (live.size > PER_ROW) repeat(PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
