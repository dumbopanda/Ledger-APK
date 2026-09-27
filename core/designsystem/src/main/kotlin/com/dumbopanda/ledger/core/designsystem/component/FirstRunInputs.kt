package com.dumbopanda.ledger.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dumbopanda.ledger.core.designsystem.CanvasDark
import com.dumbopanda.ledger.core.designsystem.Coral
import com.dumbopanda.ledger.core.designsystem.Forest
import com.dumbopanda.ledger.core.designsystem.HairlineStrong
import com.dumbopanda.ledger.core.designsystem.InactiveOnDark
import com.dumbopanda.ledger.core.designsystem.Lime
import com.dumbopanda.ledger.core.designsystem.PlaceholderInk
import com.dumbopanda.ledger.core.designsystem.TextDark
import com.dumbopanda.ledger.core.designsystem.TrackBorderOnDark
import com.dumbopanda.ledger.core.designsystem.TrackOnDark
import com.dumbopanda.ledger.core.designsystem.White
import com.dumbopanda.ledger.core.designsystem.icon.LedgerLineIcons
import com.dumbopanda.ledger.core.designsystem.webText

/**
 * The first-run flow's text field: 56 tall, 14-radius, placeholder inside, label kept OUTSIDE
 * by the caller (so it never collapses into the border the way a Material outlined label does).
 * [onDark] swaps to the dark sheet's track colours with a lime focus ring.
 */
@Composable
fun FirstRunField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    onDark: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    autofill: ContentType? = null,
    description: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val fill = if (onDark) TrackOnDark else White
    val ink = if (onDark) White else TextDark
    val hint = if (onDark) InactiveOnDark else PlaceholderInk
    val border = when {
        isError -> Coral
        focused -> if (onDark) Lime else Forest
        else -> if (onDark) TrackBorderOnDark else HairlineStrong
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        textStyle = webText(16f, FontWeight.Medium, ink),
        cursorBrush = SolidColor(if (onDark) Lime else Forest),
        interactionSource = interaction,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                if (autofill != null) contentType = autofill
                if (description != null) contentDescription = description
            },
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(fill, RoundedCornerShape(14.dp))
                    .border(if (focused || isError) 1.5.dp else 1.dp, border, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (icon != null) Icon(icon, contentDescription = null, tint = hint, modifier = Modifier.size(20.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, style = webText(16f, FontWeight.Medium, hint), maxLines = 1)
                    inner()
                }
            }
        },
    )
}

/**
 * A selectable pill for short preset answers (occupation, account suggestions). Never shrinks
 * its label -- callers lay these out in a wrapping FlowRow so a long label moves to the next
 * line instead of squeezing. Selected: dark ink with lime text on cream, lime with dark text on
 * the dark sheet -- the same pairs Home's account chips use.
 */
@Composable
fun FirstRunChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
) {
    val fill by animateColorAsState(
        when {
            selected && onDark -> Lime
            selected -> CanvasDark
            onDark -> TrackOnDark
            else -> White
        },
        label = "chipFill",
    )
    val ink = when {
        selected && onDark -> CanvasDark
        selected -> Lime
        onDark -> White
        else -> TextDark
    }
    val outline = when {
        selected -> fill
        onDark -> TrackBorderOnDark
        else -> HairlineStrong
    }
    Row(
        modifier
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .background(fill)
            .border(1.dp, outline, CircleShape)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (selected) Icon(LedgerLineIcons.Check, contentDescription = null, tint = ink, modifier = Modifier.size(16.dp))
        Text(text, style = webText(14f, FontWeight.Bold, ink), maxLines = 1, softWrap = false)
    }
}
