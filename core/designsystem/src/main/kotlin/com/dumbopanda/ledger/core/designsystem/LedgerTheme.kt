package com.dumbopanda.ledger.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun LedgerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LedgerColorScheme,
        typography = LedgerTypography,
        shapes = LedgerShapes,
        content = content,
    )
}
