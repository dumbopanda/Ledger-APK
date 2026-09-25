package com.dumbopanda.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dumbopanda.ledger.core.designsystem.component.LedgerBanner
import com.dumbopanda.ledger.core.designsystem.component.LedgerBannerTone
import com.dumbopanda.ledger.core.designsystem.component.LedgerPillButton

@Composable
fun RestoringRoute(
    onDone: () -> Unit,
    onContinueOffline: () -> Unit,
    viewModel: RestoringViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewModel.done) {
        if (viewModel.done) onDone()
    }

    RestoringScreen(
        pulledCount = viewModel.pulledCount,
        failed = viewModel.failed,
        onRetry = viewModel::retry,
        onContinueOffline = onContinueOffline,
    )
}

@Composable
internal fun RestoringScreen(
    pulledCount: Int,
    failed: Boolean,
    onRetry: () -> Unit,
    onContinueOffline: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (failed) {
                LedgerBanner(text = "Couldn't restore your ledger.", tone = LedgerBannerTone.Error)
                Spacer(Modifier.height(16.dp))
                LedgerPillButton(text = "Retry", onClick = onRetry)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onContinueOffline) { Text("Continue offline") }
            } else {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Restoring your ledger...", style = MaterialTheme.typography.bodyLarge)
                if (pulledCount > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text("$pulledCount rows so far", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
