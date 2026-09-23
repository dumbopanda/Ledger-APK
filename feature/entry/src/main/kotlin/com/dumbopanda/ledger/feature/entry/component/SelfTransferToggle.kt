package com.dumbopanda.ledger.feature.entry.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dumbopanda.ledger.core.designsystem.LedgerTheme

/**
 * §7.5.3's pill toggle. Purely presentational -- flips one boolean up; every consequence of
 * that flip (the 9 on / 5 off state changes) is
 * [com.dumbopanda.ledger.core.domain.ApplySelfTransferToggleUseCase]'s job, called by whoever
 * owns the draft, not by this component.
 */
@Composable
fun SelfTransferToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Text(
            text = "Self transfer",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(end = 12.dp),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Preview(fontScale = 1.0f)
@Preview(fontScale = 2.0f)
@Composable
private fun SelfTransferToggleOffPreview() {
    LedgerTheme {
        SelfTransferToggle(checked = false, onCheckedChange = {}, modifier = Modifier.padding(16.dp))
    }
}

@Preview(fontScale = 1.0f)
@Composable
private fun SelfTransferToggleOnPreview() {
    LedgerTheme {
        SelfTransferToggle(checked = true, onCheckedChange = {}, modifier = Modifier.padding(16.dp))
    }
}
