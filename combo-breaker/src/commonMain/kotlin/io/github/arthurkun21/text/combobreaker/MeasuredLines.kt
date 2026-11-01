package io.github.arthurkun21.text.combobreaker

import android.graphics.text.LineBreaker
import android.graphics.text.MeasuredText
import android.os.Build
import android.text.TextPaint
import androidx.annotation.RequiresApi
import androidx.compose.ui.text.TextStyle
import dev.romainguy.text.combobreaker.Interval
import dev.romainguy.text.combobreaker.MeasuredTextHelper
import dev.romainguy.text.combobreaker.TextFlowHyphenation
import kotlin.math.max

/**
 * Hyphen edit that does not affect text rendering, with the same value as the platform's
 * `Paint.START_HYPHEN_EDIT_NO_EDIT` (API 29+). Declared locally so it can be referenced on
 * API levels below 29.
 */
internal const val NO_HYPHEN_EDIT: Int = 0

/**
 * Internal result of measuring and line-breaking the remaining text of a paragraph at a
 * fixed width. Lines are consumed one at a time by the text flow layout algorithm, which
 * queries each line's break offset, metrics and hyphen edits until the paragraph is
 * exhausted.
 *
 * Implementations must only be created from the text flow layout algorithm, which selects
 * the appropriate platform engine: [LineBreaker] + [MeasuredText] on API 29 and above,
 * [StaticLayoutLines] (backed by `StaticLayout`) on API 24 to 28.
 */
internal interface MeasuredLines {
    /**
     * Returns the number of lines computed over the measured text.
     */
    fun lineCount(): Int

    /**
     * Returns the offset, relative to the start of the measured text, just past the last
     * character of the line at index [line]. If [line] is greater than or equal to the
     * number of computed lines, returns the length of the measured text to signal the end
     * of the paragraph.
     */
    fun lineBreakOffset(line: Int): Int

    /**
     * Returns the width of the line at index [line], or 0.0f if [line] is out of bounds.
     */
    fun lineWidth(line: Int): Float

    /**
     * Returns the (negative) ascent of the line at index [line], or 0.0f if [line] is out
     * of bounds. The sign convention matches [LineBreaker.Result.getLineAscent].
     */
    fun lineAscent(line: Int): Float

    /**
     * Returns the descent of the line at index [line], or 0.0f if [line] is out of bounds.
     */
    fun lineDescent(line: Int): Float

    /**
     * Returns the hyphen edit to apply at the start of the line at index [line]. Always
     * [NO_HYPHEN_EDIT] on API levels below 33 where hyphenation is not
     * computed.
     */
    fun startLineHyphenEdit(line: Int): Int

    /**
     * Returns the hyphen edit to apply at the end of the line at index [line]. Always
     * [NO_HYPHEN_EDIT] on API levels below 33 where hyphenation is not
     * computed.
     */
    fun endLineHyphenEdit(line: Int): Int

    /**
     * Returns the measured width of the text between the offsets [start] (inclusive) and
     * [end] (exclusive), relative to the start of the measured text.
     */
    fun width(start: Int, end: Int): Float

    companion object {
        /**
         * An empty measurement used to initialize the layout state before any text has
         * been measured.
         */
        val Empty: MeasuredLines = object : MeasuredLines {
            override fun lineCount(): Int = 0

            override fun lineBreakOffset(line: Int): Int = 0

            override fun lineWidth(line: Int): Float = 0.0f

            override fun lineAscent(line: Int): Float = 0.0f

            override fun lineDescent(line: Int): Float = 0.0f

            override fun startLineHyphenEdit(line: Int): Int = NO_HYPHEN_EDIT

            override fun endLineHyphenEdit(line: Int): Int = NO_HYPHEN_EDIT

            override fun width(start: Int, end: Int): Float = 0.0f
        }
    }
}

/**
 * [MeasuredLines] implementation backed by [LineBreaker] and [MeasuredText], available on
 * API 29 and above.
 */
@RequiresApi(29)
private object LineBreakerHolder {
    val lineBreaker: LineBreaker = LineBreaker.Builder()
        .setBreakStrategy(LineBreaker.BREAK_STRATEGY_HIGH_QUALITY)
        .setHyphenationFrequency(LineBreaker.HYPHENATION_FREQUENCY_FULL)
        .setJustificationMode(LineBreaker.JUSTIFICATION_MODE_NONE)
        .build()
}

/**
 * [MeasuredLines] implementation backed by [LineBreaker] and [MeasuredText], available on
 * API 29 and above. This type must only be instantiated from code guarded by a
 * `Build.VERSION.SDK_INT >= 29` check as the platform classes it relies on do not exist
 * on earlier API levels.
 *
 * @param charArray The text to measure, starting at [paragraphOffset] inside the paragraph.
 * @param styles The merged styles of the paragraph, relative to the start of the paragraph.
 * @param paragraphOffset The offset inside the paragraph at which the measured text starts.
 * @param paintForStyle Returns the paint to use to measure and draw the given style.
 * @param width The width available to break lines into.
 * @param hyphenation The hyphenation behavior to use.
 */
@RequiresApi(29)
internal class LineBreakerLines(
    charArray: CharArray,
    styles: List<Interval<TextStyle>>,
    paragraphOffset: Int,
    paintForStyle: (TextStyle) -> TextPaint,
    width: Float,
    hyphenation: TextFlowHyphenation,
) : MeasuredLines {
    private val textLength: Int = charArray.size
    private val measuredText: MeasuredText
    private val result: LineBreaker.Result

    init {
        // MeasuredText.Builder wants styles to cover the entire array, and LineBreaker
        // therefore expects to compute breaks over the entire array
        measuredText = MeasuredText.Builder(charArray)
            .appendStyleRuns(styles, paragraphOffset, paintForStyle)
            .hyphenation(hyphenation)
            .build()

        val constraints = LineBreaker.ParagraphConstraints()
        constraints.width = width

        result = LineBreakerHolder.lineBreaker.computeLineBreaks(measuredText, constraints, 0)
    }

    override fun lineCount(): Int = result.lineCount

    override fun lineBreakOffset(line: Int): Int =
        if (line >= result.lineCount) {
            textLength
        } else {
            result.getLineBreakOffset(line)
        }

    override fun lineWidth(line: Int): Float =
        if (line >= result.lineCount) 0.0f else result.getLineWidth(line)

    override fun lineAscent(line: Int): Float =
        if (line >= result.lineCount) 0.0f else result.getLineAscent(line)

    override fun lineDescent(line: Int): Float =
        if (line >= result.lineCount) 0.0f else result.getLineDescent(line)

    override fun startLineHyphenEdit(line: Int): Int =
        if (line >= result.lineCount) {
            NO_HYPHEN_EDIT
        } else {
            result.getStartLineHyphenEdit(line)
        }

    override fun endLineHyphenEdit(line: Int): Int =
        if (line >= result.lineCount) {
            NO_HYPHEN_EDIT
        } else {
            result.getEndLineHyphenEdit(line)
        }

    override fun width(start: Int, end: Int): Float = measuredText.getWidth(start, end)
}

/**
 * Append style runs to this [MeasuredText.Builder]. The style runs are provided as a list
 * of merged style intervals relative to the start of the paragraph, adjusted to the start
 * of the measured text.
 */
@RequiresApi(29)
private fun MeasuredText.Builder.appendStyleRuns(
    styles: List<Interval<TextStyle>>,
    paragraphOffset: Int,
    paintForStyle: (TextStyle) -> TextPaint,
): MeasuredText.Builder {
    styles.forEach { interval ->
        if (interval.end > paragraphOffset) {
            val start = max(0, (interval.start - paragraphOffset).toInt())
            val end = (interval.end - paragraphOffset).toInt()
            val style = interval.data!!
            appendStyleRun(paintForStyle(style), end - start, false)
        }
    }

    return this
}

/**
 * Configure hyphenation on this [MeasuredText.Builder]. Hyphenation is only computed on
 * API 33 and above (see [MeasuredTextHelper]).
 */
private fun MeasuredText.Builder.hyphenation(
    hyphenation: TextFlowHyphenation,
): MeasuredText.Builder {
    if (Build.VERSION.SDK_INT >= 33) {
        MeasuredTextHelper.hyphenation(this, hyphenation)
    }
    return this
}
