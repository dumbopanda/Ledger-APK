package com.dumbopanda.ledger.feature.entry.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dumbopanda.ledger.core.designsystem.component.GroupInset
import com.dumbopanda.ledger.core.designsystem.component.LedgerField
import com.dumbopanda.ledger.core.designsystem.component.LedgerGroup
import com.dumbopanda.ledger.core.designsystem.component.LedgerPill
import com.dumbopanda.ledger.core.designsystem.component.LedgerSelect
import com.dumbopanda.ledger.core.designsystem.component.PillStyles
import com.dumbopanda.ledger.core.domain.OwedToMeItem
import com.dumbopanda.ledger.core.domain.formatDisplayDate
import com.dumbopanda.ledger.core.domain.formatMoney
import com.dumbopanda.ledger.core.model.TxnId

/**
 * `#backGroup` -- "Will I get this back?": Yes | No; on Yes, "Which one" (New — I am lending
 * or paying out now / Old — this is money coming back); on Old, "Which open item" listing each
 * open item as "name · ₹amount · date". Hidden entirely by the caller in self transfer, whose
 * Y id is automatic.
 */
@Composable
fun WillGetBackGroup(
    txnId: TxnId,
    backOld: Boolean,
    openItems: List<OwedToMeItem>,
    onBack: (Boolean) -> Unit,
    onBackKind: (Boolean) -> Unit,
    onOpenItem: (TxnId) -> Unit,
    modifier: Modifier = Modifier,
) {
    LedgerGroup(modifier = modifier, title = "Will I get this back?") {
        Row(Modifier.fillMaxWidth().padding(start = GroupInset), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LedgerPill("Yes", selected = txnId.isY, onClick = { onBack(true) }, style = PillStyles.Compact, modifier = Modifier.weight(1f))
            LedgerPill("No", selected = !txnId.isY, onClick = { onBack(false) }, style = PillStyles.Compact, modifier = Modifier.weight(1f))
        }
        if (txnId.isY) {
            LedgerField("Which one", Modifier.padding(start = GroupInset)) {
                LedgerSelect(
                    options = listOf(false, true),
                    selected = backOld,
                    onSelect = onBackKind,
                    optionLabel = { old -> if (old) "Old — this is money coming back" else "New — I am lending or paying out now" },
                )
            }
            if (backOld) {
                LedgerField("Which open item", Modifier.padding(start = GroupInset)) {
                    LedgerSelect(
                        options = openItems,
                        selected = openItems.find { it.txnId == txnId },
                        onSelect = { onOpenItem(it.txnId) },
                        optionLabel = { "${it.expenseName} · ₹${formatMoney(it.amount.minor)} · ${formatDisplayDate(it.openedDate)}" },
                        placeholder = "Nothing open right now",
                    )
                }
            }
        }
    }
}
