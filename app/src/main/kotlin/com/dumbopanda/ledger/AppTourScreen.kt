package com.dumbopanda.ledger

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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dumbopanda.ledger.core.designsystem.Amber
import com.dumbopanda.ledger.core.designsystem.AmberDeepInk
import com.dumbopanda.ledger.core.designsystem.CanvasDark
import com.dumbopanda.ledger.core.designsystem.CanvasLight
import com.dumbopanda.ledger.core.designsystem.Coral
import com.dumbopanda.ledger.core.designsystem.Forest
import com.dumbopanda.ledger.core.designsystem.HairlineLt
import com.dumbopanda.ledger.core.designsystem.LedgerTheme
import com.dumbopanda.ledger.core.designsystem.Lime
import com.dumbopanda.ledger.core.designsystem.Mint
import com.dumbopanda.ledger.core.designsystem.MintInk
import com.dumbopanda.ledger.core.designsystem.MutedOnDark
import com.dumbopanda.ledger.core.designsystem.SystemChrome
import com.dumbopanda.ledger.core.designsystem.SystemChromeEffect
import com.dumbopanda.ledger.core.designsystem.TextDark
import com.dumbopanda.ledger.core.designsystem.TextMuted
import com.dumbopanda.ledger.core.designsystem.White
import com.dumbopanda.ledger.core.designsystem.component.DisplayHeadline
import com.dumbopanda.ledger.core.designsystem.component.LimeButton
import com.dumbopanda.ledger.core.designsystem.component.PageDots
import com.dumbopanda.ledger.core.designsystem.component.webBorder
import com.dumbopanda.ledger.core.designsystem.icon.LedgerIcons
import com.dumbopanda.ledger.core.designsystem.icon.LedgerLineIcons
import com.dumbopanda.ledger.core.designsystem.webText

@Composable
fun AppTourRoute(onDone: () -> Unit, viewModel: AppTourViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    if (uiState is AppTourUiState.AlreadySeen || uiState is AppTourUiState.Done) {
        LaunchedEffect(uiState) { onDone() }
        return
    }
    AppTourScreen(uiState = uiState, onNext = viewModel::next, onBack = viewModel::back, onSkip = viewModel::skip)
}

private data class TourPane(
    val eyebrow: String,
    val headline: String,
    val accent: String,
    val body: String,
    val cta: String,
    val visual: @Composable () -> Unit,
)

private val Panes = listOf(
    TourPane(
        eyebrow = "Add Payment",
        headline = "Start here.",
        accent = "Every rupee begins as one line.",
        body = "Type what moved and pick the account it moved through. If it's money you " +
            "expect back, mark “Will I get this back?” and Ledger keeps that tab open " +
            "until it's settled.",
        cta = "Next",
        visual = { AddPaymentMock() },
    ),
    TourPane(
        eyebrow = "History",
        headline = "Everything,",
        accent = "sorted by day.",
        body = "Every entry lands here, grouped by date. Tap one to edit or delete it. Flip to " +
            "“Owed to me” to see just the loans still open, and the ones already settled.",
        cta = "Next",
        visual = { HistoryMock() },
    ),
    TourPane(
        eyebrow = "Stats",
        headline = "Your balances,",
        accent = "without doing the math.",
        body = "What's in each account, what you've spent, what's left. All of it " +
            "updated the moment you save an entry.",
        cta = "Next",
        visual = { StatsMock() },
    ),
    TourPane(
        eyebrow = "Your profile",
        headline = "One tap,",
        accent = "for everything else.",
        body = "This icon opens your accounts, backup and security. Add or rename an account, " +
            "change how often Ledger backs up, or lock it behind your fingerprint.",
        cta = "Name my accounts",
        visual = { ProfileMock() },
    ),
)

@Composable
internal fun AppTourScreen(uiState: AppTourUiState, onNext: () -> Unit, onBack: () -> Unit, onSkip: () -> Unit) {
    val pane = (uiState as? AppTourUiState.Ready)?.pane ?: return
    BackHandler(enabled = pane > 0, onBack = onBack)
    SystemChromeEffect(SystemChrome.Dark)

    BoxWithConstraints(Modifier.fillMaxSize().background(CanvasDark)) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "How Ledger works",
                    style = webText(13f, FontWeight.Bold, MutedOnDark, letterSpacing = 0.4f),
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onSkip).padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Skip tour", style = webText(14f, FontWeight.SemiBold, MutedOnDark)) }
            }

            AnimatedContent(
                targetState = pane,
                transitionSpec = {
                    val forward = targetState > initialState
                    (fadeIn(tween(260)) + slideInHorizontally(tween(260)) { if (forward) it / 8 else -it / 8 }) togetherWith fadeOut(tween(160))
                },
                label = "tourPane",
                modifier = Modifier.weight(1f, fill = false),
            ) { current ->
                val data = Panes[current]
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) { data.visual() }
                    Text(data.eyebrow.uppercase(), style = webText(12f, FontWeight.ExtraBold, Lime, letterSpacing = 1.2f))
                    Spacer(Modifier.height(8.dp))
                    DisplayHeadline(data.headline, data.accent, size = 28f, modifier = Modifier.semantics { heading() })
                    Spacer(Modifier.height(10.dp))
                    Text(data.body, style = webText(15f, FontWeight.Normal, MutedOnDark, lineHeight = 22f))
                }
            }

            Column {
                Spacer(Modifier.height(20.dp))
                PageDots(APP_TOUR_PANE_COUNT, pane, onDark = true)
                Spacer(Modifier.height(20.dp))
                LimeButton(Panes[pane].cta, onNext, trailingIcon = LedgerLineIcons.ArrowRight)
            }
        }
    }
}

// A muted (35%-opacity) region of the mock screen -- everything that ISN'T this pane's subject.
@Composable
private fun Dim(content: @Composable () -> Unit) = Box(Modifier.alpha(0.35f)) { content() }

// The one thing each pane is actually about: full brightness, with a soft lime glow behind it.
@Composable
private fun Spotlight(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier.drawBehind {
            drawRoundRect(
                Brush.radialGradient(listOf(Lime.copy(alpha = 0.28f), Color.Transparent), radius = size.maxDimension * 0.75f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx()),
            )
        },
    ) { content() }
}

@Composable
private fun MockCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .width(300.dp)
            .background(White, RoundedCornerShape(22.dp))
            .webBorder(HairlineLt, RoundedCornerShape(22.dp))
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun MockTabs(active: Int) {
    Row(Modifier.background(TextDark, CircleShape).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("Add Payment", "History", "Stats").forEachIndexed { i, label ->
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(if (i == active) Lime else Color.Transparent)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) { Text(label, style = webText(12f, FontWeight.Bold, if (i == active) TextDark else MutedOnDark)) }
        }
    }
}

@Composable
private fun MockTag(text: String, fill: Color, ink: Color) {
    Text(text, style = webText(10f, FontWeight.Bold, ink), modifier = Modifier.background(fill, CircleShape).padding(horizontal = 8.dp, vertical = 3.dp))
}

// ---- Pane 1: Add Payment -------------------------------------------------------------------

@Composable
private fun AddPaymentMock() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Dim { MockTabs(active = 0) }
        Spacer(Modifier.height(14.dp))
        Spotlight {
            MockCard {
                Text("AMOUNT", style = webText(11f, FontWeight.Bold, TextMuted, letterSpacing = 0.5f))
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("₹", style = webText(28f, FontWeight.ExtraBold, TextDark))
                    Spacer(Modifier.width(6.dp))
                    Text("640", style = webText(28f, FontWeight.ExtraBold, TextDark))
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.background(TextDark, CircleShape).padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text("Debit", style = webText(12f, FontWeight.Bold, Lime))
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Dim {
            MockCard {
                Text("WILL I GET THIS BACK?", style = webText(10f, FontWeight.Bold, TextMuted, letterSpacing = 0.4f))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.background(TextDark, CircleShape).padding(horizontal = 16.dp, vertical = 8.dp)) { Text("Yes", style = webText(12f, FontWeight.Bold, Lime)) }
                    Box(Modifier.background(CanvasLight, CircleShape).padding(horizontal = 16.dp, vertical = 8.dp)) { Text("No", style = webText(12f, FontWeight.Bold, TextDark)) }
                }
            }
        }
    }
}

// ---- Pane 2: History ------------------------------------------------------------------------

@Composable
private fun HistoryMock() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Dim { MockTabs(active = 1) }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Dim { Box(Modifier.background(CanvasLight, CircleShape).padding(horizontal = 14.dp, vertical = 8.dp)) { Text("Recent", style = webText(12f, FontWeight.Bold, TextMuted)) } }
            Spotlight { Box(Modifier.background(TextDark, CircleShape).padding(horizontal = 14.dp, vertical = 8.dp)) { Text("Owed to me", style = webText(12f, FontWeight.Bold, Lime)) } }
        }
        Spacer(Modifier.height(12.dp))
        Dim {
            MockCard {
                Row(Modifier.fillMaxWidth()) {
                    Text("Today", style = webText(13f, FontWeight.Bold, TextDark), modifier = Modifier.weight(1f))
                    Text("−₹640", style = webText(13f, FontWeight.Bold, Coral))
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Groceries", style = webText(14f, FontWeight.Bold, TextDark))
                        Spacer(Modifier.height(4.dp))
                        MockTag("Cash", CanvasLight, TextMuted)
                    }
                    Text("−₹640", style = webText(14f, FontWeight.ExtraBold, Coral))
                }
            }
        }
    }
}

// ---- Pane 3: Stats --------------------------------------------------------------------------

@Composable
private fun StatsMock() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Dim { MockTabs(active = 2) }
        Spacer(Modifier.height(14.dp))
        Spotlight {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Cash", "₹2,140", Mint, MintInk)
                StatTile("Total spent", "₹1,690", Amber, AmberDeepInk)
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, fill: Color, ink: Color) {
    Column(
        Modifier.width(140.dp).background(White, RoundedCornerShape(18.dp)).webBorder(HairlineLt, RoundedCornerShape(18.dp)).padding(14.dp),
    ) {
        Box(Modifier.size(8.dp).background(fill, CircleShape))
        Spacer(Modifier.height(8.dp))
        Text(label, style = webText(11f, FontWeight.SemiBold, TextMuted))
        Spacer(Modifier.height(2.dp))
        Text(value, style = webText(17f, FontWeight.ExtraBold, TextDark))
    }
}

// ---- Pane 4: Profile / Settings -------------------------------------------------------------

@Composable
private fun ProfileMock() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.width(300.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { Dim { Text("Ledger", style = webText(18f, FontWeight.ExtraBold, White)) } }
            Spotlight {
                Box(Modifier.size(44.dp).background(TextDark, CircleShape).webBorder(Lime, CircleShape, width = 1.5.dp), contentAlignment = Alignment.Center) {
                    Icon(LedgerIcons.Settings, contentDescription = null, tint = Lime, modifier = Modifier.size(20.dp))
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Dim {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MockTag("Accounts", CanvasLight, TextDark)
                MockTag("Sync speed", CanvasLight, TextDark)
                MockTag("Security", CanvasLight, TextDark)
            }
        }
    }
}

@Preview(heightDp = 900)
@Composable
private fun AppTourScreenPreview() {
    LedgerTheme {
        AppTourScreen(uiState = AppTourUiState.Ready(0), onNext = {}, onBack = {}, onSkip = {})
    }
}
