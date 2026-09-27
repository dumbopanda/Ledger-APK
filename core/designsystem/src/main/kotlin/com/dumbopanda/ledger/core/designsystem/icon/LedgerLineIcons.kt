package com.dumbopanda.ledger.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The first-run flow's outline icons: 24-unit grid, 2-unit round-capped strokes, drawn from the
 * same path data as the design canvas so the app and the mockup can't drift. Tint them with
 * `Icon(tint = …)` like any Material icon. (material-icons-core has no outline set, and pulling
 * in the extended pack for a dozen glyphs isn't worth its size.)
 */
object LedgerLineIcons {
    val ArrowRight = line("ArrowRight", "M5 12h14M13 6l6 6-6 6")
    val ChevronLeft = line("ChevronLeft", "M15 5l-7 7 7 7")
    val Plus = line("Plus", "M12 5v14M5 12h14")
    val Mail = line("Mail", "M5 5h14a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z", "M3 7l9 6 9-6")
    val Check = line("Check", "M5 12.5l4.5 4.5L19 7.5")
    val CloudUp = line("CloudUp", "M7 18a4 4 0 0 1-.5-7.97A6 6 0 0 1 18 9a4.5 4.5 0 0 1-.5 9z", "M12 11v5M9.5 13.5L12 16l2.5-2.5")
    val CloudCheck = line("CloudCheck", "M7 18a4 4 0 0 1-.5-7.97A6 6 0 0 1 18 9a4.5 4.5 0 0 1-.5 9z", "M9.5 13.5l2 2 3.5-4")
    val Download = line("Download", "M12 4v11M7 10l5 5 5-5M5 20h14")
    val Pin = line("Pin", "M12 21s-7-6.2-7-11a7 7 0 0 1 14 0c0 4.8-7 11-7 11z", "M9.5 10a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0")
    val Shield = line("Shield", "M12 3l7 3v5c0 5-3.5 8.5-7 10-3.5-1.5-7-5-7-10V6z", "M9 12l2 2 4-4")
    val Trash = line("Trash", "M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13")
    val Return = line("Return", "M9 14l-4-4 4-4", "M5 10h10a4 4 0 0 1 0 8h-3")
    val Clock = line("Clock", "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0", "M12 7v5l3 2")
    val External = line("External", "M14 4h6v6M20 4l-9 9M18 14v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1h5")

    private fun line(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .apply {
                paths.forEach { d ->
                    addPath(
                        pathData = addPathNodes(d),
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    )
                }
            }
            .build()
}
