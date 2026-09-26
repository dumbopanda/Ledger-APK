package com.dumbopanda.ledger.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dumbopanda.ledger.core.designsystem.TextDark
import com.dumbopanda.ledger.core.designsystem.White
import com.dumbopanda.ledger.core.designsystem.webText
import kotlinx.coroutines.delay

/** One app-wide toast, like index.html's single `#toast` element: the latest message wins. */
@Stable
class LedgerToastState {
    internal var message by mutableStateOf("")
    internal var serial by mutableIntStateOf(0)
    internal var durationMs = 2200L

    fun show(text: String, durationMs: Long = 2200L) {
        message = text
        this.durationMs = durationMs
        serial++
    }
}

val LocalLedgerToast = staticCompositionLocalOf { LedgerToastState() }

@Composable
fun rememberLedgerToastState(): LedgerToastState = remember { LedgerToastState() }

/**
 * `.toast` -- a dark pill, 14/600 white, centred 110px above the bottom, fading in and out over
 * 250ms. Never takes touches (pointer-events: none).
 */
@Composable
fun LedgerToastHost(state: LedgerToastState, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(state.serial) {
        if (state.serial == 0) return@LaunchedEffect
        visible = true
        delay(state.durationMs)
        visible = false
    }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(250), label = "toast")
    if (alpha == 0f) return
    Box(modifier.fillMaxSize()) {
        Text(
            state.message,
            style = webText(14f, FontWeight.SemiBold, White),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 20.dp, end = 20.dp, bottom = 110.dp)
                .alpha(alpha)
                .widthIn(max = 420.dp)
                .clip(CircleShape)
                .background(TextDark)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}
