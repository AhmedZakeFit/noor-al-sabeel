package io.github.ahmedzakefit.noor.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/*
 * Architectural ornaments for the Noor Al-Sabeel identity.
 *
 * These are drawn with the geometry of real Islamic architecture rather
 * than stock iconography: a two-centred pointed mihrab arch, and the
 * khatim — the eight-pointed star formed by two overlapping squares that
 * underlies centuries of zellige tilework. Both are quiet: they appear on
 * one hero surface and as tiny marks, never wallpapered across the app.
 */

/**
 * A pointed mihrab arch: the classic two-centred arch of Islamic portals
 * rising over a softly rounded base. The arch circles are centred on the
 * springline and pass exactly through the spring points and the apex, the
 * way a real arch is laid out.
 */
class MihrabArchShape(
    /** Arch rise as a fraction of the shape's width. */
    private val archFraction: Float = 0.30f,
    /** Corner radius of the base. */
    private val corner: Dp = 26.dp,
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return Outline.Generic(Path())

        val cornerPx = with(density) { corner.toPx() }
            .coerceIn(0f, minOf(w, h) / 4f)
        val archH = (w * archFraction)
            .coerceIn(0f, minOf(h * 0.55f, w * 0.45f))

        // Two-centred arch: centre offset from each side so the circle
        // passes through (0, archH), (w, archH) and the apex (w/2, 0).
        val cx = w / 4f + (archH * archH) / w
        val r = cx
        // Angle of the apex as seen from the left circle's centre.
        val apexDeg = Math.toDegrees(
            kotlin.math.atan2(archH.toDouble(), (w / 2f - cx).toDouble()),
        ).toFloat()
        // Clockwise sweep from the 180° spring point up over the apex.
        val sweep = 180f - apexDeg

        val path = Path().apply {
            moveTo(0f, archH)
            // Left half of the arch.
            arcTo(
                Rect(cx - r, archH - r, cx + r, archH + r),
                180f,
                sweep,
                false,
            )
            // Right half (mirrored centre) lands on (w, archH).
            arcTo(
                Rect(w - cx - r, archH - r, w - cx + r, archH + r),
                180f + apexDeg,
                sweep,
                false,
            )
            lineTo(w, h - cornerPx)
            arcTo(
                Rect(w - 2 * cornerPx, h - 2 * cornerPx, w, h),
                0f,
                90f,
                false,
            )
            lineTo(cornerPx, h)
            arcTo(
                Rect(0f, h - 2 * cornerPx, 2 * cornerPx, h),
                90f,
                90f,
                false,
            )
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * A faint khatim lattice: eight-pointed stars (two overlapping squares)
 * drawn as fine outlines in a staggered grid. Applied over a solid
 * background colour it reads as woven texture, not as a sticker.
 */
fun Modifier.zelligeLattice(
    color: Color,
    cell: Dp = 34.dp,
    alpha: Float = 0.07f,
    strokeWidth: Dp = 1.dp,
): Modifier = drawWithContent {
    val c = cell.toPx()
    val sw = strokeWidth.toPx()
    if (c > 4f && sw > 0f) {
        val starRadius = c * 0.34f
        val lattice = Path()
        var row = 0
        var y = -c
        while (y < size.height + c) {
            val xOffset = if (row % 2 == 0) 0f else c / 2f
            var x = -c + xOffset
            while (x < size.width + c) {
                lattice.addKhatim(x + c / 2f, y + c / 2f, starRadius)
                x += c
            }
            y += c * 0.64f
            row++
        }
        drawPath(lattice, color = color, alpha = alpha, style = Stroke(width = sw))
    }
    drawContent()
}

/** Appends one khatim star (two squares, 45° apart) to [this] path. */
private fun Path.addKhatim(cx: Float, cy: Float, circumradius: Float) {
    for (base in intArrayOf(45, 0)) {
        var angle = Math.toRadians(base.toDouble())
        val step = Math.PI / 2
        val p0 = Offset(
            (cx + circumradius * cos(angle)).toFloat(),
            (cy + circumradius * sin(angle)).toFloat(),
        )
        moveTo(p0.x, p0.y)
        repeat(3) {
            angle += step
            lineTo(
                (cx + circumradius * cos(angle)).toFloat(),
                (cy + circumradius * sin(angle)).toFloat(),
            )
        }
        close()
    }
}

/**
 * A small filled khatim star — used as the "next prayer" marker in lists,
 * where a plain dot would say nothing about the identity.
 */
@androidx.compose.runtime.Composable
fun StarMark(
    starSize: Dp,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(starSize)) {
        val r = this.size.minDimension / 2f
        val path = Path()
        path.addKhatim(center.x, center.y, r)
        drawPath(path, color = color)
    }
}
