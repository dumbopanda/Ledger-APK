package com.dumbopanda.ledger.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dumbopanda.ledger.core.designsystem.Amber
import com.dumbopanda.ledger.core.designsystem.Coral
import com.dumbopanda.ledger.core.designsystem.LedgerShapes
import com.dumbopanda.ledger.core.designsystem.LedgerTheme
import com.dumbopanda.ledger.core.designsystem.Mint
import com.dumbopanda.ledger.core.designsystem.TextDark
import com.dumbopanda.ledger.core.designsystem.icon.LedgerIcons

enum class LedgerBannerTone { Info, Warning, Error }

// A persistent inline notice the user must notice, e.g. the "session expired,
// showing local data" note at Home (§7.1) — stays until dismissed or resolved.
@Composable
fun LedgerBanner(
    text: String,
    tone: LedgerBannerTone,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    val background = when (tone) {
        LedgerBannerTone.Info -> Mint
        LedgerBannerTone.Warning -> Amber
        LedgerBannerTone.Error -> Coral
    }
    val content = if (tone == LedgerBannerTone.Error) MaterialTheme.colorScheme.onError else TextDark

    // .update-bar/.backup-bar/.sync-bar all round to --radius-input.
    Surface(shape = LedgerShapes.extraSmall, color = background, contentColor = content, modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (onDismiss != null) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(LedgerIcons.Close, contentDescription = "Dismiss")
                }
            }
        }
    }
}

@Preview(fontScale = 1.0f)
@Preview(fontScale = 2.0f)
@Composable
private fun LedgerBannerPreview() {
    LedgerTheme {
        LedgerBanner(
            text = "Session expired — showing local data",
            tone = LedgerBannerTone.Warning,
            onDismiss = {},
        )
    }
}
