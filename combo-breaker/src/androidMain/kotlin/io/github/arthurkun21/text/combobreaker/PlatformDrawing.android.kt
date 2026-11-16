package io.github.arthurkun21.text.combobreaker

import android.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import dev.romainguy.text.combobreaker.TextSegment

/**
 * Draws a text segment through the platform canvas, applying the segment's hyphen edits
 * and word spacing to the paint on API 29 and above. Below API 29, where the paint
 * hyphen edit and word spacing APIs do not exist, words are drawn one by one with the
 * segment's justification width added to the advance of each stretchable space.
 *
 * Hyphen edits are never applied below API 29: hyphenation is only computed on API 33+,
 * so hyphen edits are always `NO_HYPHEN_EDIT`.
 */
internal actual fun DrawScope.drawTextSegment(segment: TextSegment) {
    drawIntoCanvas { canvas ->
        val c = canvas.nativeCanvas
        val paint = segment.paint.paint

        if (supportsWordSpacing) {
            with(segment) {
                paint.startHyphenEdit = startHyphen
                paint.endHyphenEdit = endHyphen
                paint.wordSpacing = justifyWidth

                c.drawText(buffer, start, end, x, y, paint)
            }
        } else {
            c.drawTextPreApi29(segment)
        }
    }
}

/**
 * Draws a text segment word by word, adding the segment's justify width to the advance
 * of each stretchable space (U+0020) to mirror the behavior of the platform's word
 * spacing.
 */
private fun Canvas.drawTextPreApi29(segment: TextSegment) {
    with(segment) {
        val textPaint = paint.paint

        if (justifyWidth == 0.0f) {
            drawText(buffer, start, end, x, y, textPaint)
            return
        }

        var cursor = start
        var wordStart = start
        var cursorX = x

        while (cursor < end) {
            if (buffer[cursor] == ' ') {
                if (wordStart < cursor) {
                    drawText(buffer, wordStart, cursor, cursorX, y, textPaint)
                    cursorX += textPaint.measureText(buffer, wordStart, cursor)
                }
                cursorX += textPaint.measureText(buffer, cursor, cursor + 1) + justifyWidth
                wordStart = cursor + 1
            }
            cursor++
        }

        if (wordStart < end) {
            drawText(buffer, wordStart, end, cursorX, y, textPaint)
        }
    }
}
