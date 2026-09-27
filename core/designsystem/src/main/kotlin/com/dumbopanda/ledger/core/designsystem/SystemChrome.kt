package com.dumbopanda.ledger.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * What shows behind the status bar ([top]) and the gesture/navigation bar ([bottom]). The app
 * root pads every screen clear of both bars, so a screen can't paint under them itself -- the
 * first-run flow's dark hero and cream sheets set this instead, and everything else keeps the
 * cream default. [lightIcons] = white status-bar icons, for a dark [top].
 */
data class SystemChrome(val top: Color, val bottom: Color, val lightIcons: Boolean) {
    companion object {
        val Default = SystemChrome(top = CanvasLight, bottom = CanvasLight, lightIcons = false)
        val Dark = SystemChrome(top = CanvasDark, bottom = CanvasDark, lightIcons = true)
        // Dark hero on top, cream sheet running down to the bottom edge.
        val DarkOverCream = SystemChrome(top = CanvasDark, bottom = CanvasLight, lightIcons = true)
    }
}

/**
 * Holds the current [SystemChrome] and which screen set it. The owner matters because during a
 * navigation the incoming screen composes BEFORE the outgoing one is disposed: without it, the
 * old screen's cleanup would reset the bars right after the new screen set them (seen live:
 * Onboarding → Sign in left a cream status bar under white icons).
 */
class SystemChromeController {
    var chrome by mutableStateOf(SystemChrome.Default)
        private set
    private var owner: Any? = null

    internal fun claim(key: Any, value: SystemChrome) {
        owner = key
        chrome = value
    }

    internal fun release(key: Any) {
        if (owner !== key) return
        owner = null
        chrome = SystemChrome.Default
    }
}

val LocalSystemChrome = staticCompositionLocalOf<SystemChromeController?> { null }

/** Holds [chrome] while this screen is composed; puts the default back when it leaves, unless
 *  another screen has claimed the bars since. */
@Composable
fun SystemChromeEffect(chrome: SystemChrome) {
    val controller = LocalSystemChrome.current ?: return
    val key = remember { Any() }
    DisposableEffect(controller, chrome) {
        controller.claim(key, chrome)
        onDispose { controller.release(key) }
    }
}
