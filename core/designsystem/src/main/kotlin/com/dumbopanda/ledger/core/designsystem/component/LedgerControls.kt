package com.dumbopanda.ledger.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dumbopanda.ledger.core.designsystem.CanvasLight
import com.dumbopanda.ledger.core.designsystem.ChevronInk
import com.dumbopanda.ledger.core.designsystem.Coral
import com.dumbopanda.ledger.core.designsystem.Forest
import com.dumbopanda.ledger.core.designsystem.HairlineLt
import com.dumbopanda.ledger.core.designsystem.HairlineStrong
import com.dumbopanda.ledger.core.designsystem.LEDGER_UI_SCALE
import com.dumbopanda.ledger.core.designsystem.LedgerShapes
import com.dumbopanda.ledger.core.designsystem.LedgerUiScale
import com.dumbopanda.ledger.core.designsystem.Lime
import com.dumbopanda.ledger.core.designsystem.SwitchOff
import com.dumbopanda.ledger.core.designsystem.TextDark
import com.dumbopanda.ledger.core.designsystem.TextMuted
import com.dumbopanda.ledger.core.designsystem.White
import com.dumbopanda.ledger.core.designsystem.webText

// The web has no tap highlight (-webkit-tap-highlight-color: transparent) on any of these
// controls -- their pressed/selected states are the feedback, not a Material ripple.
fun Modifier.webClickable(enabled: Boolean = true, role: Role? = Role.Button, onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled,
        role = role,
        onClick = onClick,
    )
}

// A CSS `border: 1px solid` -- drawn on the edge AND taking its 1px in layout, which Compose's
// own border() doesn't (it paints over the padding instead). Without this every bordered box
// comes out 2px smaller than index.html's.
fun Modifier.webBorder(color: Color, shape: Shape, width: Dp = 1.dp): Modifier = border(width, color, shape).padding(width)

// `.group > * { padding-left: 5px }` -- every child of a card except its title sits 5px in.
val GroupInset: Dp = 5.dp

/**
 * `.group` -- a white, 1px-bordered, 22px-radius card, 16px padding, 14px between children,
 * with an optional `.group-title` (16/700, -0.2px, margin-bottom -2px). [overlay] is for
 * absolutely-positioned extras like "Which account"'s Self transfer pill (top 16, right 16).
 */
@Composable
fun LedgerGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .background(White, LedgerShapes.medium)
            .webBorder(HairlineLt, LedgerShapes.medium),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (title != null) GroupTitle(title)
            content()
        }
        overlay()
    }
}

@Composable
fun GroupTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = webText(16f, FontWeight.Bold, letterSpacing = -0.2f),
        modifier = modifier.layout { measurable, constraints ->
            // margin-bottom: -2px
            val placeable = measurable.measure(constraints)
            layout(placeable.width, (placeable.height - 2.dp.roundToPx()).coerceAtLeast(0)) { placeable.place(0, 0) }
        },
    )
}

/** `.field label` -- 13/600 muted, 8px above its control. */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier, suffix: String? = null) {
    Row(modifier) {
        Text(text, style = webText(13f, FontWeight.SemiBold, TextMuted))
        if (suffix != null) Text(" $suffix", style = webText(13f, FontWeight.Medium, TextMuted))
    }
}

@Composable
fun LedgerField(label: String, modifier: Modifier = Modifier, labelSuffix: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(label, suffix = labelSuffix)
        content()
    }
}

@Immutable
data class PillStyle(
    val padding: PaddingValues,
    val text: TextStyle,
    val idleFill: Color,
    val idleInk: Color,
    val bordered: Boolean,
    val minHeight: Dp = 0.dp,
)

object PillStyles {
    // `.chip` inside a card
    val Chip = PillStyle(PaddingValues(horizontal = 16.dp, vertical = 10.dp), webText(14f, FontWeight.SemiBold), CanvasLight, TextDark, true)

    // `.kind-btn` (Credit/Debit, Yes/No) and `.account-block .chip`
    val Compact = PillStyle(PaddingValues(horizontal = 8.dp, vertical = 12.dp), webText(12f, FontWeight.Bold), CanvasLight, TextDark, true)

    // `.seg-chips button` (Batched / Instant / Manual)
    val SegChip = PillStyle(PaddingValues(horizontal = 4.dp, vertical = 11.dp), webText(12f, FontWeight.Bold), CanvasLight, TextDark, true)

    // `.quick-date-btn` (Today / Yesterday)
    val QuickDate = PillStyle(PaddingValues(horizontal = 12.dp), webText(12f, FontWeight.SemiBold), CanvasLight, TextMuted, true, 40.dp)

    // `.toggle-pill` (Self transfer)
    val Toggle = PillStyle(PaddingValues(horizontal = 12.dp), webText(12f, FontWeight.SemiBold), CanvasLight, TextMuted, true, 34.dp)

    // `.seg button` (Stats' By name / By category / By account) -- no border, sits on a track
    val Segment = PillStyle(PaddingValues(vertical = 8.dp), webText(12f, FontWeight.Bold), Color.Transparent, TextMuted, false)

    // `.leg-btn` (entry detail's Open / Close)
    val Leg = PillStyle(PaddingValues(horizontal = 10.dp, vertical = 5.dp), webText(11f, FontWeight.Bold), Color.Transparent, TextMuted, false)
}

/**
 * Every selectable pill in index.html shares one rule: selected is dark (`--text-dark`) with
 * lime text; idle is the style's own fill/ink with a 1px `--border`. [locked] is `.kind-btn
 * .locked` -- shown, dimmed to 85%, not tappable.
 */
@Composable
fun LedgerPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PillStyle = PillStyles.Chip,
    locked: Boolean = false,
    leading: String? = null,
) {
    val fill = if (selected) TextDark else style.idleFill
    val ink = if (selected) Lime else style.idleInk
    val border = if (selected) TextDark else HairlineLt
    Box(
        modifier = modifier
            .alpha(if (locked) 0.85f else 1f)
            .heightIn(min = style.minHeight)
            .clip(CircleShape)
            .background(fill)
            .then(if (style.bordered) Modifier.webBorder(border, CircleShape) else Modifier)
            .webClickable(enabled = !locked, onClick = onClick)
            .padding(style.padding),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            if (leading != null) Text(leading, style = style.text.copy(color = ink, fontSize = 13.sp))
            Text(text, style = style.text.copy(color = ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// The 14x9 arrow index.html draws as a background-image on every <select>.
@Composable
fun SelectChevron(modifier: Modifier = Modifier) {
    Canvas(modifier.size(14.dp, 9.dp)) {
        val s = size.width / 14f
        val path = Path().apply {
            moveTo(1f * s, 1f * s)
            lineTo(7f * s, 7f * s)
            lineTo(13f * s, 1f * s)
        }
        drawPath(path, ChevronInk, style = Stroke(width = 2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private fun inputBorder(invalid: Boolean, focused: Boolean): Color = when {
    invalid -> Coral
    focused -> Forest
    else -> HairlineLt
}

@Composable
private fun InputDecoration(
    isEmpty: Boolean,
    placeholder: String?,
    textStyle: TextStyle,
    fill: Color,
    borderColor: Color,
    contentPadding: PaddingValues,
    trailing: (@Composable () -> Unit)?,
    inner: @Composable () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(fill, LedgerShapes.extraSmall)
            .webBorder(borderColor, LedgerShapes.extraSmall),
    ) {
        Box(Modifier.fillMaxWidth().padding(contentPadding).padding(end = if (trailing != null) 22.dp else 0.dp)) {
            if (isEmpty && placeholder != null) {
                Text(placeholder, style = textStyle.copy(color = ChevronInk), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            inner()
        }
        if (trailing != null) Box(Modifier.align(Alignment.CenterEnd).fillMaxHeight()) { trailing() }
    }
}

/**
 * `input[type=text]` inside a card: cream fill (`.group input` swaps white for canvas-light),
 * 1px border that turns green on focus and red when `.is-invalid`, 14px radius, 14/16 padding,
 * 16/500 text.
 */
@Composable
fun LedgerInput(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    invalid: Boolean = false,
    textStyle: TextStyle = webText(16f, FontWeight.Medium),
    fill: Color = CanvasLight,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    onFocusChange: (Boolean) -> Unit = {},
    trailing: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().onFocusChanged { onFocusChange(it.isFocused) },
        singleLine = true,
        textStyle = textStyle,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        interactionSource = interaction,
        cursorBrush = SolidColor(TextDark),
        decorationBox = { inner ->
            InputDecoration(value.text.isEmpty(), placeholder, textStyle, fill, inputBorder(invalid, focused), contentPadding, trailing, inner)
        },
    )
}

@Composable
fun LedgerInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    invalid: Boolean = false,
    textStyle: TextStyle = webText(16f, FontWeight.Medium),
    fill: Color = CanvasLight,
    trailing: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    // The web blanks some placeholders on focus and restores them on blur if still empty.
    hidePlaceholderWhileFocused: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = textStyle,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        interactionSource = interaction,
        cursorBrush = SolidColor(TextDark),
        visualTransformation = visualTransformation,
        decorationBox = { inner ->
            InputDecoration(
                value.isEmpty(), placeholder.takeUnless { hidePlaceholderWhileFocused && focused }, textStyle, fill, inputBorder(invalid, focused),
                PaddingValues(horizontal = 16.dp, vertical = 14.dp), trailing, inner,
            )
        },
    )
}

@Composable
private fun <T> SelectMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    widthPx: Int,
    options: List<T>,
    optionLabel: (T) -> String,
    optionItalic: (T) -> Boolean,
    onSelect: (T) -> Unit,
) {
    // The menu is its own popup window, laid out at the device's real density rather than the
    // app's 0.9-scaled one -- so its width is converted at the real density to match the field
    // exactly, and its items get the web's zoom back via LedgerUiScale.
    val realDensity = LocalDensity.current.density / LEDGER_UI_SCALE
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = White,
        modifier = Modifier.width((widthPx / realDensity).dp),
    ) {
        LedgerUiScale { SelectMenuItems(options, optionLabel, optionItalic, onDismiss, onSelect) }
    }
}

@Composable
private fun <T> SelectMenuItems(
    options: List<T>,
    optionLabel: (T) -> String,
    optionItalic: (T) -> Boolean,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
) {
    Column {
        options.forEach { option ->
            DropdownMenuItem(
                text = {
                    Text(
                        optionLabel(option),
                        style = webText(16f, FontWeight.Medium, italic = optionItalic(option)),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                onClick = {
                    onDismiss()
                    onSelect(option)
                },
            )
        }
    }
}

/**
 * `<select>` inside a card -- looks exactly like [LedgerInput] (cream fill, border, 14px
 * radius, 16/500), with the arrow at `right 16px`, and opens its options on tap.
 */
@Composable
fun <T> LedgerSelect(
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    invalid: Boolean = false,
    optionItalic: (T) -> Boolean = { false },
    textStyle: TextStyle = webText(16f, FontWeight.Medium),
) {
    var expanded by remember { mutableStateOf(false) }
    var widthPx by remember { mutableIntStateOf(0) }
    Box(modifier.fillMaxWidth().onGloballyPositioned { widthPx = it.size.width }) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(LedgerShapes.extraSmall)
                .background(CanvasLight)
                .webBorder(inputBorder(invalid, expanded), LedgerShapes.extraSmall)
                .webClickable { expanded = true }
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                selected?.let(optionLabel) ?: placeholder,
                style = textStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            SelectChevron()
        }
        SelectMenu(expanded, { expanded = false }, widthPx, options, optionLabel, optionItalic, onSelect)
    }
}

/**
 * `.combo-wrap` -- a real select normally; once "Custom…" / "Add a new name…" is chosen, the
 * select "shrinks down to an invisible strip over just the arrow" and a text input takes its
 * place, so the arrow still reopens the same list to switch back.
 */
@Composable
fun <T> LedgerComboField(
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    optionLabel: (T) -> String,
    customMode: Boolean,
    customValue: String,
    onCustomValueChange: (String) -> Unit,
    customPlaceholder: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    invalid: Boolean = false,
    optionItalic: (T) -> Boolean = { false },
    isCustomOption: (T) -> Boolean = { false },
    customInputModifier: Modifier = Modifier,
    hideCustomPlaceholderWhileFocused: Boolean = false,
) {
    // Choosing the custom option puts the caret straight into the new text input, as the web's
    // change handler does (`customInput.focus()`). Only a pick from the list does this -- a
    // custom value restored for an edit opens the input without grabbing the keyboard.
    val customFocus = remember { FocusRequester() }
    var focusCustom by remember { mutableStateOf(false) }
    val pick: (T) -> Unit = { option ->
        if (isCustomOption(option)) focusCustom = true
        onSelect(option)
    }
    LaunchedEffect(customMode, focusCustom) {
        if (customMode && focusCustom) {
            customFocus.requestFocus()
            focusCustom = false
        }
    }

    if (!customMode) {
        LedgerSelect(options, selected, pick, optionLabel, modifier, placeholder, invalid, optionItalic)
        return
    }
    var expanded by remember { mutableStateOf(false) }
    var widthPx by remember { mutableIntStateOf(0) }
    Box(modifier.fillMaxWidth().onGloballyPositioned { widthPx = it.size.width }) {
        LedgerInput(
            value = customValue,
            onValueChange = onCustomValueChange,
            modifier = Modifier.focusRequester(customFocus).then(customInputModifier),
            placeholder = customPlaceholder,
            invalid = invalid,
            hidePlaceholderWhileFocused = hideCustomPlaceholderWhileFocused,
            trailing = {
                Box(
                    Modifier.width(44.dp).fillMaxHeight().webClickable { expanded = true },
                    contentAlignment = Alignment.CenterEnd,
                ) { SelectChevron(Modifier.padding(end = 16.dp)) }
            },
        )
        SelectMenu(expanded, { expanded = false }, widthPx, options, optionLabel, optionItalic, pick)
    }
}

/** `.switch` -- 46x28, green when on, 22px white knob sliding 18px. */
@Composable
fun LedgerSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val offset by animateDpAsState(if (checked) 18.dp else 0.dp, tween(200), label = "knob")
    val track by animateColorAsState(if (checked) Forest else SwitchOff, tween(200), label = "track")
    Box(
        modifier
            .size(46.dp, 28.dp)
            .clip(CircleShape)
            .background(track)
            // toggleable, not clickable: screen readers announce it as an on/off switch.
            .toggleable(
                value = checked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
    ) {
        Box(
            Modifier
                .padding(3.dp)
                .offset(x = offset)
                .size(22.dp)
                .shadow(1.dp, CircleShape)
                .background(White, CircleShape),
        )
    }
}

/** `.mini-btn` / `.mini-btn.primary`, including both disabled looks. */
@Composable
fun LedgerMiniButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    danger: Boolean = false,
) {
    val fill: Color
    val border: Color
    val ink: Color
    when {
        primary && enabled -> { fill = TextDark; border = TextDark; ink = Lime }
        primary -> { fill = CanvasLight; border = HairlineLt; ink = TextMuted }
        danger -> { fill = White; border = Coral; ink = Coral }
        else -> { fill = CanvasLight; border = HairlineStrong; ink = TextDark }
    }
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.55f)
            .clip(CircleShape)
            .background(fill)
            .webBorder(border, CircleShape)
            .webClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = webText(13f, FontWeight.Bold, ink), maxLines = 1)
    }
}

enum class SheetButtonKind { Save, Outline, Danger }

/** `.btn` -- `.btn-save` (green), `.btn-outline`, `.btn-danger`. */
@Composable
fun LedgerSheetButton(
    text: String,
    kind: SheetButtonKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val (fill, ink, border) = when (kind) {
        SheetButtonKind.Save -> Triple(Forest, White, null)
        SheetButtonKind.Outline -> Triple(White, TextDark, HairlineLt)
        SheetButtonKind.Danger -> Triple(White, Coral, Coral)
    }
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.6f)
            .clip(CircleShape)
            .background(fill)
            .then(if (border != null) Modifier.webBorder(border, CircleShape) else Modifier)
            .webClickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = webText(16f, FontWeight.Bold, ink), maxLines = 1)
    }
}

/** `.cta-save` -- full-width green pill, lime dot, 17/700. */
@Composable
fun LedgerCta(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Row(
        modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.7f)
            .clip(CircleShape)
            .background(Forest)
            .webClickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 18.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(22.dp).background(Lime, CircleShape))
        Text(text, style = webText(17f, FontWeight.Bold, White), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** `.update-bar` / `.backup-bar` / `.sync-bar` -- 14px-radius strip, 12/14/12/16 padding. */
@Composable
fun LedgerNoticeBar(
    fill: Color,
    modifier: Modifier = Modifier,
    border: Color? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(LedgerShapes.extraSmall)
            .background(fill)
            .then(if (border != null) Modifier.webBorder(border, LedgerShapes.extraSmall) else Modifier)
            .padding(start = 16.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/** The small pill buttons inside those bars (`.update-btn`, `.backup-btn`, `.sync-btn`). */
@Composable
fun LedgerBarButton(
    text: String,
    onClick: () -> Unit,
    fill: Color,
    ink: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.6f)
            .clip(CircleShape)
            .background(fill)
            .webClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = webText(13f, FontWeight.Bold, ink), maxLines = 1)
    }
}
