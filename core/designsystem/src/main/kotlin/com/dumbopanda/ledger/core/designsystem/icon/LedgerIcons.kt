package com.dumbopanda.ledger.core.designsystem.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings

object LedgerIcons {
    val Add = Icons.Filled.Add
    val Back = Icons.AutoMirrored.Filled.ArrowBack
    val Close = Icons.Filled.Close
    val Confirm = Icons.Filled.Check
    val Delete = Icons.Filled.Delete
    val Regenerate = Icons.Filled.Refresh
    val Settings = Icons.Filled.Settings
    // §7.12's reorder buttons (material-icons-core only -- no extended pack in this project).
    val MoveUp = Icons.Filled.KeyboardArrowUp
    val MoveDown = Icons.Filled.KeyboardArrowDown
}
