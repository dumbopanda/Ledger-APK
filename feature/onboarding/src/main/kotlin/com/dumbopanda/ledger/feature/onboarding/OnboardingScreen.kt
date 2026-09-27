package com.dumbopanda.ledger.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dumbopanda.ledger.core.designsystem.AmberDeepInk
import com.dumbopanda.ledger.core.designsystem.AmberInk
import com.dumbopanda.ledger.core.designsystem.Amber
import com.dumbopanda.ledger.core.designsystem.CanvasDark
import com.dumbopanda.ledger.core.designsystem.CanvasLight
import com.dumbopanda.ledger.core.designsystem.CashFill
import com.dumbopanda.ledger.core.designsystem.CashInk
import com.dumbopanda.ledger.core.designsystem.Coral
import com.dumbopanda.ledger.core.designsystem.EditedFill
import com.dumbopanda.ledger.core.designsystem.EditedInk
import com.dumbopanda.ledger.core.designsystem.Forest
import com.dumbopanda.ledger.core.designsystem.HairlineLt
import com.dumbopanda.ledger.core.designsystem.LedgerTheme
import com.dumbopanda.ledger.core.designsystem.Lime
import com.dumbopanda.ledger.core.designsystem.Mint
import com.dumbopanda.ledger.core.designsystem.MintInk
import com.dumbopanda.ledger.core.designsystem.MutedOnDark
import com.dumbopanda.ledger.core.designsystem.NetOutOnDark
import com.dumbopanda.ledger.core.designsystem.SystemChrome
import com.dumbopanda.ledger.core.designsystem.SystemChromeEffect
import com.dumbopanda.ledger.core.designsystem.TextDark
import com.dumbopanda.ledger.core.designsystem.TextMuted
import com.dumbopanda.ledger.core.designsystem.White
import com.dumbopanda.ledger.core.designsystem.component.DisplayHeadline
import com.dumbopanda.ledger.core.designsystem.component.IconBadge
import com.dumbopanda.ledger.core.designsystem.component.LedgerBrandMark
import com.dumbopanda.ledger.core.designsystem.component.LedgerCta
import com.dumbopanda.ledger.core.designsystem.component.LedgerHeroSheet
import com.dumbopanda.ledger.core.designsystem.component.LimeButton
import com.dumbopanda.ledger.core.designsystem.component.PageDots
import com.dumbopanda.ledger.core.designsystem.component.webBorder
import com.dumbopanda.ledger.core.designsystem.icon.LedgerLineIcons
import com.dumbopanda.ledger.core.designsystem.webText
import com.dumbopanda.ledger.core.ui.openUrl

private const val PANE_COUNT = 3
private const val CONSENT_PANE = 2
private const val PRIVACY_POLICY_URL = "https://dumbopanda.github.io/Ledger-Pages/privacy/"

@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    OnboardingScreen(
        onAgreeAndContinue = { viewModel.agreeAndContinue(onFinished) },
        onOpenPrivacyPolicy = { context.openUrl(PRIVACY_POLICY_URL) },
    )
}

/**
 * §7.2's three panes, told as one story: the promise (every rupee and every IOU remembered),
 * the reason to trust it (saved instantly, with a backup file as the spare key -- the old
 * "Google-only, no recovery" warning, kept honest), then the consent notice as four plain rows.
 * Panes 1-2 skip straight to consent; consent itself is never skipped.
 */
@Composable
internal fun OnboardingScreen(onAgreeAndContinue: () -> Unit, onOpenPrivacyPolicy: () -> Unit) {
    var pane by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = pane > 0) { pane -= 1 }
    SystemChromeEffect(if (pane == CONSENT_PANE) SystemChrome.DarkOverCream else SystemChrome.Dark)

    AnimatedContent(
        targetState = pane,
        transitionSpec = {
            val forward = targetState > initialState
            (fadeIn(tween(260)) + slideInHorizontally(tween(260)) { if (forward) it / 8 else -it / 8 }) togetherWith
                fadeOut(tween(160))
        },
        label = "onboardingPane",
    ) { current ->
        when (current) {
            0 -> StoryPane(
                index = 0,
                lead = "Every rupee, remembered.",
                accent = "Every IOU, too.",
                body = "The chai at the stall. The metro recharge. The ₹450 a friend swore they'd pay " +
                    "back. Log each in seconds. Ledger keeps the tab.",
                cta = "Get started",
                onNext = { pane = 1 },
                onSkip = { pane = CONSENT_PANE },
            ) { DayCardVisual() }
            1 -> StoryPane(
                index = 1,
                lead = "Saved the moment\nyou tap",
                accent = "Save.",
                breakBeforeAccent = false,
                body = "Every entry lands on your phone first, then backs itself up. There's no " +
                    "password to reset, so export a backup file now and then. It's your spare key.",
                cta = "Next",
                onNext = { pane = CONSENT_PANE },
                onSkip = { pane = CONSENT_PANE },
            ) { SavedVisual() }
            else -> ConsentPane(onAgreeAndContinue = onAgreeAndContinue, onOpenPrivacyPolicy = onOpenPrivacyPolicy)
        }
    }
}

@Composable
private fun StoryPane(
    index: Int,
    lead: String,
    accent: String,
    body: String,
    cta: String,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    breakBeforeAccent: Boolean = true,
    visual: @Composable () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(CanvasDark)) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                LedgerBrandMark(36.dp)
                Spacer(Modifier.width(10.dp))
                Text("Ledger", style = webText(18f, FontWeight.ExtraBold, White, letterSpacing = -0.3f), modifier = Modifier.weight(1f))
                Box(
                    Modifier.heightIn(min = 48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onSkip).padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Skip", style = webText(14f, FontWeight.SemiBold, MutedOnDark)) }
            }

            Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(320.dp).background(Brush.radialGradient(listOf(Lime.copy(alpha = 0.2f), Color.Transparent)), CircleShape))
                visual()
            }

            Column {
                DisplayHeadline(lead, accent, breakBeforeAccent = breakBeforeAccent, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(12.dp))
                Text(body, style = webText(15f, FontWeight.Normal, MutedOnDark, lineHeight = 22f))
                Spacer(Modifier.height(24.dp))
                PageDots(PANE_COUNT, index, onDark = true)
                Spacer(Modifier.height(20.dp))
                LimeButton(cta, onNext, trailingIcon = LedgerLineIcons.ArrowRight)
            }
        }
    }
}

// Pane 1's picture: a real-looking History day card, with the IOU the headline promises.
@Composable
private fun DayCardVisual() {
    Box(
        Modifier.width(340.dp).clearAndSetSemantics {
            contentDescription = "Example day in Ledger: groceries, a metro recharge, and ₹450 lent to a friend, still owed"
        },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .width(310.dp)
                .rotate(-2f)
                .shadow(24.dp, RoundedCornerShape(22.dp))
                .background(White, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp)),
        ) {
            Row(Modifier.fillMaxWidth().background(TextDark).padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("Today", style = webText(13f, FontWeight.Bold, White), modifier = Modifier.weight(1f))
                Text("−₹1,690", style = webText(13f, FontWeight.Bold, NetOutOnDark))
            }
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                SampleEntry("Groceries", listOf(Tag("Cash", CashFill, CashInk)), "−₹640", Coral, divider = true)
                SampleEntry("Metro recharge", listOf(Tag("Domestic", Mint, MintInk)), "−₹600", Coral, divider = true)
                SampleEntry(
                    "Lent to Riya · dinner",
                    listOf(Tag("Domestic", Mint, MintInk), Tag("Owed to you", Amber, AmberDeepInk)),
                    "₹450",
                    AmberInk,
                    divider = false,
                )
            }
        }
        Row(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(y = 20.dp)
                .shadow(12.dp, CircleShape)
                .background(Lime, CircleShape)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(LedgerLineIcons.Plus, contentDescription = null, tint = CanvasDark, modifier = Modifier.size(16.dp))
            Text("Add entry", style = webText(13f, FontWeight.ExtraBold, CanvasDark))
        }
    }
}

private data class Tag(val label: String, val fill: Color, val ink: Color)

@Composable
private fun SampleEntry(name: String, tags: List<Tag>, amount: String, amountColor: Color, divider: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (divider) Modifier.bottomHairline() else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(name, style = webText(14f, FontWeight.Bold, TextDark))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                tags.forEach { tag ->
                    Text(
                        tag.label,
                        style = webText(10f, FontWeight.Bold, tag.ink),
                        modifier = Modifier.background(tag.fill, CircleShape).padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
        Text(amount, style = webText(15f, FontWeight.ExtraBold, amountColor))
    }
}

// Pane 2's picture: where an entry goes, in order.
@Composable
private fun SavedVisual() {
    Column(
        Modifier
            .width(310.dp)
            .shadow(24.dp, RoundedCornerShape(22.dp))
            .background(White, RoundedCornerShape(22.dp))
            .padding(horizontal = 18.dp, vertical = 6.dp),
    ) {
        SavedRow(LedgerLineIcons.Check, Mint, Forest, "Saved on your phone", "Groceries · −₹640", divider = true)
        SavedRow(LedgerLineIcons.CloudUp, EditedFill, EditedInk, "Backed up to your account", "On its own, whenever you're online", divider = true)
        SavedRow(LedgerLineIcons.Download, CanvasLight, TextDark, "Your spare key", "ledger-backup-2026-09-27.json", divider = false)
    }
}

@Composable
private fun SavedRow(icon: ImageVector, fill: Color, ink: Color, title: String, detail: String, divider: Boolean) {
    Row(
        Modifier.fillMaxWidth().then(if (divider) Modifier.bottomHairline() else Modifier).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBadge(icon, fill, ink)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = webText(14f, FontWeight.Bold, TextDark))
            Text(detail, style = webText(12f, FontWeight.Normal, TextMuted))
        }
    }
}

// Pane 3: §7.2's consent notice. Same facts as before (email + Google ID if Google, Supabase
// ap-south-1, never sold, deletable from Settings), just scannable.
@Composable
private fun ConsentPane(onAgreeAndContinue: () -> Unit, onOpenPrivacyPolicy: () -> Unit) {
    LedgerHeroSheet(
        hero = {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 30.dp)) {
                LedgerBrandMark(44.dp)
                Spacer(Modifier.height(22.dp))
                DisplayHeadline("Your money is", "nobody's business.", size = 32f, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(10.dp))
                Text("No ads. No card offers. Here's everything Ledger keeps.", style = webText(15f, FontWeight.Normal, MutedOnDark, lineHeight = 22f))
            }
        },
        sheetPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 28.dp),
        sheetTop = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(White, RoundedCornerShape(22.dp))
                    .webBorder(HairlineLt, RoundedCornerShape(22.dp))
                    .padding(horizontal = 18.dp, vertical = 4.dp),
            ) {
                DataRow(LedgerLineIcons.Mail, "Your email address", "It's how you sign in. A Google ID too, if you use Google.", divider = true)
                DataRow(LedgerLineIcons.Pin, "Kept in India", "On Supabase servers in Mumbai (ap-south-1).", divider = true)
                DataRow(LedgerLineIcons.Shield, "Never sold, never shared", "No trackers. No device fingerprinting.", divider = true)
                DataRow(LedgerLineIcons.Trash, "Yours to delete", "Settings → Delete account wipes it all, for good.", divider = false)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .heightIn(min = 48.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onOpenPrivacyPolicy),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Read the full Privacy Policy", style = webText(14f, FontWeight.Bold, Forest))
                Icon(LedgerLineIcons.External, contentDescription = null, tint = Forest, modifier = Modifier.size(16.dp))
            }
        },
        sheetBottom = {
            PageDots(PANE_COUNT, CONSENT_PANE, onDark = false, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(18.dp))
            LedgerCta("Agree and continue", onAgreeAndContinue)
        },
    )
}

@Composable
private fun DataRow(icon: ImageVector, title: String, detail: String, divider: Boolean) {
    Row(
        Modifier.fillMaxWidth().then(if (divider) Modifier.bottomHairline() else Modifier).padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconBadge(icon, Mint, Forest)
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = webText(14f, FontWeight.Bold, TextDark))
            Text(detail, style = webText(13f, FontWeight.Normal, TextMuted, lineHeight = 18f))
        }
    }
}

// The 1px rule between rows in a card, drawn along the row's bottom edge.
private fun Modifier.bottomHairline(): Modifier = drawBehind {
    val stroke = 1.dp.toPx()
    drawRect(HairlineLt, topLeft = Offset(0f, size.height - stroke), size = Size(size.width, stroke))
}

@Preview(heightDp = 900)
@Composable
private fun OnboardingScreenPreview() {
    LedgerTheme {
        OnboardingScreen(onAgreeAndContinue = {}, onOpenPrivacyPolicy = {})
    }
}
