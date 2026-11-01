package io.github.arthurkun21.text.combobreaker

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import dev.romainguy.text.combobreaker.TextSegment

/**
 * Draws a text segment through the Compose text machinery: the segment is measured with
 * the segment's paint and drawn with [DrawScope.drawText] at the segment's position.
 *
 * Word spacing is not natively supported on this platform: when the segment requires
 * justification or line shrinking, words are drawn one by one, adding the segment's
 * justify width to the advance of each stretchable space (U+0020) to mirror the behavior
 * of platforms without word spacing support.
 *
 * Hyphen edits are never applied: hyphenation is not computed on this platform, so hyphen
 * edits are always `NO_HYPHEN_EDIT`.
 */
internal actual fun DrawScope.drawTextSegment(segment: TextSegment) {
    val paint = segment.paint

    if (segment.justifyWidth == 0.0f) {
        drawSegmentText(segment, segment.start, segment.end, segment.x)
        return
    }

    var cursor = segment.start
    var wordStart = segment.start
    var cursorX = segment.x

    while (cursor < segment.end) {
        if (segment.buffer[cursor] == ' ') {
            if (wordStart < cursor) {
                drawSegmentText(segment, wordStart, cursor, cursorX)
                cursorX += paint.measureText(segment.buffer, wordStart, cursor)
            }
            cursorX += paint.measureText(segment.buffer, cursor, cursor + 1) + segment.justifyWidth
            wordStart = cursor + 1
        }
        cursor++
    }

    if (wordStart < segment.end) {
        drawSegmentText(segment, wordStart, segment.end, cursorX)
    }
}

/**
 * Measures and draws the text between [start] and [end] at the baseline position
 * ([x], [segment.y]) using the segment's paint.
 */
private fun DrawScope.drawSegmentText(segment: TextSegment, start: Int, end: Int, x: Float) {
    if (start >= end) return

    val paint = segment.paint
    val style = paint.style
    val layout = paint.textMeasurer.measure(
        AnnotatedString(segment.buffer.substring(start, end), style.toSpanStyle()),
        style,
        softWrap = false,
        maxLines = 1,
    )

    val topLeft = Offset(x, segment.y - layout.getLineBaseline(0))
    val brush = style.brush
    if (brush != null) {
        drawText(layout, brush = brush, topLeft = topLeft)
    } else {
        drawText(
            layout,
            color = if (style.color == Color.Unspecified) Color.Black else style.color,
            topLeft = topLeft,
        )
    }
}
