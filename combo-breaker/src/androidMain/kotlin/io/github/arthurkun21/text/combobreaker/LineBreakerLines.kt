package io.github.arthurkun21.text.combobreaker

import android.graphics.text.LineBreaker
import android.graphics.text.MeasuredText
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.text.TextStyle
import dev.romainguy.text.combobreaker.Interval
import dev.romainguy.text.combobreaker.MeasuredTextHelper
import dev.romainguy.text.combobreaker.TextFlowHyphenation
import kotlin.math.max

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
    paintForStyle: (TextStyle) -> PlatformTextPaint,
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
    paintForStyle: (TextStyle) -> PlatformTextPaint,
): MeasuredText.Builder {
    styles.forEach { interval ->
        if (interval.end > paragraphOffset) {
            val start = max(0, (interval.start - paragraphOffset).toInt())
            val end = (interval.end - paragraphOffset).toInt()
            val style = interval.data!!
            appendStyleRun(paintForStyle(style).paint, end - start, false)
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
