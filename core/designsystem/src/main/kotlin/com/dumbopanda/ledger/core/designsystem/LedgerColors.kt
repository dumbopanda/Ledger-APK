package com.dumbopanda.ledger.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val CanvasLight = Color(0xFFF4F1EA)
val CanvasDark = Color(0xFF121614)
// The page itself is cream (CanvasLight); every card floats a shade lighter on
// top of it in true white -- "inside a card, white fields on a white surface
// would vanish, so the fill and the page swap: cream controls on white."
val White = Color(0xFFFFFFFF)
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

// --border-strong: the heavier rule for when a border separates two whole objects
// (a day card, a stat tile) rather than two rows inside one -- History's day cards
// and Stats' tiles use this instead of the default hairline.
val HairlineStrong = Color(0xFFD4D0C4)

// .top-tab's own muted label color on the dark nav track -- distinct from TextMuted,
// which is calibrated for muted text on the light/white surfaces instead.
val MutedOnDark = Color(0xFF9EAD9C)

// .day-head .net.out -- a soft coral for a negative day-total against the dark header
// band, distinct from Coral (the light-surface error/expense color).
val NetOutOnDark = Color(0xFFFF9B9B)

// The web front end's remaining one-off inks and fills, named for where index.html uses them.
val HintGrey = Color(0xFF6B6861) // .field-hint
val ChevronInk = Color(0xFF78756E) // the select/combo arrow's own stroke
val AmberInk = Color(0xFF8A6D1F) // .amt-note, .sheet-note, .date-hint.is-warn, .eb-sub
val AmberDeepInk = Color(0xFF6E571A) // .eb-title, .tag.held, .tag.acct-International
val UpdateInk = Color(0xFF5A4708) // .update-label
val MintInk = Color(0xFF14614A) // .tag.acct
val CashFill = Color(0xFFEAE5D8) // .tile.acct-Cash
val CashInk = Color(0xFF6B6045) // .tag.acct-Cash
val EditedFill = Color(0xFFFFE9C9) // .tag.edited
val EditedInk = Color(0xFF8A5A0F)
val SwitchOff = Color(0xFFD8D5CB) // .slider (off)
val CountOnDark = Color(0xFF2C312E) // .sub-tab.active .count

// First-run flow (onboarding + sign-in): the dark hero surfaces sit on CanvasDark.
val InactiveOnDark = Color(0xFFC9D3C7) // idle segment of the Sign in / Sign up pill
val PlaceholderInk = Color(0xFF8A877F) // input placeholder and leading-icon ink on white fields
val TrackOnDark = Color(0x0FFFFFFF) // 6% white: pill tracks and ghost buttons on CanvasDark
val TrackBorderOnDark = Color(0x1FFFFFFF) // 12% white: their 1px border

/**
 * The web tints tiles/tags/dots by account NAME (Domestic mint, International amber, Cash
 * stone). The APK has N user-named accounts, so the same three tones are handed out by the
 * account's position instead -- the default Domestic/International/Cash order lands on
 * exactly the web's colours, and a 4th account onward cycles.
 */
data class AccountTone(val fill: Color, val ink: Color)

private val AccountTones = listOf(
    AccountTone(Mint, MintInk),
    AccountTone(Amber, AmberDeepInk),
    AccountTone(CashFill, CashInk),
)

fun accountTone(position: Int): AccountTone = AccountTones[position.mod(AccountTones.size)]

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
    // Cards (.group/.amount-card/.panel/.stat-tile/.ledger-card) are white, distinct
    // from the cream page -- NOT the same tone, or every card would blend invisibly
    // into the background with only its 1px border for definition.
    surface = White,
    onSurface = TextDark,
    // The recessed cream fill used for input-like elements sitting on a white card
    // (.group input/select/.chip:not(.selected) etc.) -- this IS meant to equal the
    // page background, unlike `surface` above.
    surfaceVariant = CanvasLight,
    onSurfaceVariant = TextMuted,
    outline = HairlineLt,
    outlineVariant = HairlineLt,
)
