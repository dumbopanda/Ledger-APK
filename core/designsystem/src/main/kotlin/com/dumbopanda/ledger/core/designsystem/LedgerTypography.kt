package com.dumbopanda.ledger.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp

// Fetched from Google Play services at first use and cached system-wide; while
// unresolved (or if Play services is absent) Compose renders the platform
// default typeface, so this is never a network-only fetch (§6.3).
private val InterProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

// The web front end's own font (index.html's Google Fonts <link>, weights 400-800).
private val PlusJakartaSans = GoogleFont("Plus Jakarta Sans")

@OptIn(ExperimentalTextApi::class)
val LedgerFontFamily = FontFamily(
    Font(googleFont = PlusJakartaSans, fontProvider = InterProvider, weight = FontWeight.Normal),
    Font(googleFont = PlusJakartaSans, fontProvider = InterProvider, weight = FontWeight.Medium),
    Font(googleFont = PlusJakartaSans, fontProvider = InterProvider, weight = FontWeight.SemiBold),
    Font(googleFont = PlusJakartaSans, fontProvider = InterProvider, weight = FontWeight.Bold),
    Font(googleFont = PlusJakartaSans, fontProvider = InterProvider, weight = FontWeight.ExtraBold),
)

val LedgerTypography = Typography(
    headlineLarge = TextStyle(fontFamily = LedgerFontFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = LedgerFontFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    // App title (.topbar h1: 22px/800) and section-card titles (.group-title: 16px/700).
    titleLarge = TextStyle(fontFamily = LedgerFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontFamily = LedgerFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = (-0.2).sp),
    titleSmall = TextStyle(fontFamily = LedgerFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = LedgerFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = LedgerFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = LedgerFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = LedgerFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = LedgerFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = LedgerFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)
