package com.dumbopanda.ledger.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dumbopanda.ledger.core.designsystem.LedgerShapes
import com.dumbopanda.ledger.core.designsystem.LedgerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = LedgerShapes.large,
        modifier = modifier,
    ) {
        Column(Modifier.padding(24.dp), content = content)
    }
}

@Preview(fontScale = 1.0f)
@Preview(fontScale = 2.0f)
@Composable
private fun LedgerBottomSheetPreview() {
    LedgerTheme {
        Column(Modifier.padding(24.dp)) {
            Text("Add Entry • ₹4,500.00 Out", style = MaterialTheme.typography.titleMedium)
        }
    }
}
