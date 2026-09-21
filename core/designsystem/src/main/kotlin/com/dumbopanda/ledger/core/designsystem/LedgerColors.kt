package com.dumbopanda.ledger.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val CanvasLight = Color(0xFFF4F1EA)
val CanvasDark = Color(0xFF121614)
val Forest = Color(0xFF0E7955)
val Lime = Color(0xFFCCF244)
val Mint = Color(0xFFDDF3E4)
val Amber = Color(0xFFFFDE73)
// Branding doc's #E04848 is ~3.6:1 on CanvasLight, failing WCAG AA -- and
// it's the color of every expense amount. Darkened to clear 4.98:1 (§6.3).
val Coral = Color(0xFFC62828)
val TextDark = Color(0xFF181C1A)

// #78756E on CanvasLight is ~3.9:1, failing WCAG AA for body text (§6.3, §10.5).
// Darkened here rather than restricted to 14sp+ semibold everywhere it's used.
val TextMuted = Color(0xFF5F5C56)
val HairlineLt = Color(0xFFE5E3DB)

// Light theme only at launch (§6.3) — no dark surface ramp is specified, and a
// half-done dark mode on a finance app reads as broken.
val LedgerColorScheme: ColorScheme = lightColorScheme(
    primary = Forest,
    onPrimary = CanvasLight,
    secondary = Lime,
    onSecondary = TextDark,
    tertiary = Mint,
    onTertiary = TextDark,
    error = Coral,
    onError = CanvasLight,
    background = CanvasLight,
    onBackground = TextDark,
    surface = CanvasLight,
    onSurface = TextDark,
    surfaceVariant = Mint,
    onSurfaceVariant = TextMuted,
    outline = HairlineLt,
    outlineVariant = HairlineLt,
)
