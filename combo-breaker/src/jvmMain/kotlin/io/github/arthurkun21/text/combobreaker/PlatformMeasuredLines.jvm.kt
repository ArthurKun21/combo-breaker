package io.github.arthurkun21.text.combobreaker

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import dev.romainguy.text.combobreaker.Interval
import dev.romainguy.text.combobreaker.TextFlowHyphenation
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Measures and breaks the remaining text of a paragraph at the specified width using a
 * Compose [androidx.compose.ui.text.TextMeasurer], whose desktop implementation is backed
 * by the platform's text stack.
 *
 * Hyphenation is not computed on this platform: hyphen edits are always [NO_HYPHEN_EDIT],
 * mirroring the behavior of Android API levels below 33.
 *
 * @param paragraph The text to measure, starting at [paragraphOffset] inside the paragraph.
 * @param styles The merged styles of the paragraph, relative to the start of the paragraph.
 * @param paragraphOffset The offset inside the paragraph at which the measured text starts.
 * @param paintForStyle Returns the paint to use to measure and draw the given style.
 * @param width The width available to break lines into.
 */
internal class TextMeasurerLines(
    paragraph: String,
    styles: List<Interval<TextStyle>>,
    paragraphOffset: Int,
    paintForStyle: (TextStyle) -> PlatformTextPaint,
    width: Float,
) : MeasuredLines {
    private val text: String = paragraph.substring(paragraphOffset)
    private val layout: TextLayoutResult

    init {
        // The merged styles always cover the entire paragraph contiguously and always
        // contain at least one style, so the first style provides the paragraph's base
        // style and its paint provides the measurer
        val basePaint = paintForStyle(styles.first().data!!)

        val spans = styles.mapNotNull { interval ->
            if (interval.end <= paragraphOffset) return@mapNotNull null

            val start = max(0, interval.start.toInt() - paragraphOffset)
            val end = min(text.length, interval.end.toInt() - paragraphOffset)
            if (start >= end && text.isNotEmpty()) return@mapNotNull null

            AnnotatedString.Range(paintForStyle(interval.data!!).style.toSpanStyle(), start, end)
        }

        val annotated = AnnotatedString(text, spans)
        val constraints = if (width.isInfinite()) {
            Constraints()
        } else {
            Constraints(maxWidth = ceil(width).toInt())
        }

        layout = basePaint.textMeasurer.measure(
            annotated,
            basePaint.style,
            constraints = constraints,
        )
    }

    override fun lineCount(): Int = layout.multiParagraph.lineCount

    override fun lineBreakOffset(line: Int): Int =
        if (line >= lineCount()) {
            text.length
        } else {
            layout.getLineEnd(line, visibleEnd = false)
        }

    override fun lineWidth(line: Int): Float =
        if (line >= lineCount()) 0.0f else layout.getLineRight(line) - layout.getLineLeft(line)

    override fun lineAscent(line: Int): Float =
        if (line >= lineCount()) 0.0f else -(layout.getLineBaseline(line) - layout.getLineTop(line))

    override fun lineDescent(line: Int): Float =
        if (line >= lineCount()) 0.0f else layout.getLineBottom(line) - layout.getLineBaseline(line)

    // Hyphen edits are only computed by engines that support hyphenation
    override fun startLineHyphenEdit(line: Int): Int = NO_HYPHEN_EDIT

    override fun endLineHyphenEdit(line: Int): Int = NO_HYPHEN_EDIT

    /**
     * Returns the width of the text between [start] and [end], computed from the horizontal
     * positions of the offsets inside the layout. Ranges spanning multiple lines are summed
     * line by line. This assumes a left-to-right layout direction.
     */
    override fun width(start: Int, end: Int): Float {
        if (start >= end) return 0.0f

        val firstLine = layout.getLineForOffset(start)
        val lastLine = layout.getLineForOffset(end - 1)
        if (firstLine == lastLine) {
            return abs(layout.getHorizontalPosition(end, true) - layout.getHorizontalPosition(start, true))
        }

        var total = 0.0f
        var cursor = start
        for (line in firstLine..lastLine) {
            val from = max(cursor, layout.getLineStart(line))
            val to = min(end, layout.getLineEnd(line, visibleEnd = false))
            if (from < to) {
                total += abs(layout.getHorizontalPosition(to, true) - layout.getHorizontalPosition(from, true))
                cursor = to
            }
        }
        return total
    }
}

/**
 * Selects the platform line-breaking engine: a Compose [androidx.compose.ui.text.TextMeasurer]
 * backed implementation on the JVM.
 */
internal actual fun createMeasuredLines(
    paragraph: String,
    styles: List<Interval<TextStyle>>,
    paragraphOffset: Int,
    paintForStyle: (TextStyle) -> PlatformTextPaint,
    width: Float,
    hyphenation: TextFlowHyphenation,
): MeasuredLines = TextMeasurerLines(paragraph, styles, paragraphOffset, paintForStyle, width)
