package io.github.arthurkun21.text.combobreaker

import android.annotation.SuppressLint
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import androidx.compose.ui.text.TextStyle
import dev.romainguy.text.combobreaker.Interval
import dev.romainguy.text.combobreaker.trimEndSpace
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * [MeasuredLines] implementation backed by `StaticLayout`, used on API levels below 29
 * where `android.graphics.text.LineBreaker` and `android.graphics.text.MeasuredText` are
 * unavailable. The text is measured by attaching one [MetricAffectingSpan] per merged
 * style, each copying the style's cached [TextPaint] onto the layout's work paint.
 *
 * The layout is built with hyphenation disabled to match the API 29+ engine, where
 * hyphenation is only computed on API 33 and above.
 *
 * @param paragraph The text to measure, starting at [paragraphOffset] inside the paragraph.
 * @param styles The merged styles of the paragraph, relative to the start of the paragraph.
 * @param paragraphOffset The offset inside the paragraph at which the measured text starts.
 * @param paintForStyle Returns the paint to use to measure and draw the given style.
 * @param width The width available to break lines into.
 */
internal class StaticLayoutLines(
    paragraph: String,
    styles: List<Interval<TextStyle>>,
    paragraphOffset: Int,
    paintForStyle: (TextStyle) -> TextPaint,
    width: Float,
) : MeasuredLines {
    private val text: String
    private val spannable: SpannableString
    private val styleRuns: List<StyleRun>
    private val layout: StaticLayout

    init {
        text = paragraph
        spannable = SpannableString(paragraph)

        // The merged styles always cover the entire paragraph contiguously, so the runs
        // below cover the entire measured text as well
        val runs = ArrayList<StyleRun>()
        styles.forEach { interval ->
            if (interval.end > paragraphOffset) {
                val start = max(0, (interval.start - paragraphOffset).toInt())
                val end = (interval.end - paragraphOffset).toInt()
                val paint = paintForStyle(interval.data!!)
                runs += StyleRun(start, end, paint)
                spannable.setSpan(StylePaintSpan(paint), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        styleRuns = runs

        layout = buildLayout(styleRuns.first().paint, ceil(width).toInt())
    }

    override fun lineCount(): Int = layout.lineCount

    override fun lineBreakOffset(line: Int): Int =
        if (line >= layout.lineCount) {
            spannable.length
        } else {
            layout.getLineEnd(line)
        }

    override fun lineWidth(line: Int): Float {
        if (line >= layout.lineCount) return 0.0f

        val lineStart = layout.getLineStart(line)
        val lineEnd = lineBreakOffset(line)
        return width(lineStart, trimEndSpace(text, lineStart, lineEnd))
    }

    override fun lineAscent(line: Int): Float =
        if (line >= layout.lineCount) 0.0f else layout.getLineAscent(line).toFloat()

    override fun lineDescent(line: Int): Float =
        if (line >= layout.lineCount) 0.0f else layout.getLineDescent(line).toFloat()

    // Hyphen edits are only computed by the API 29+ engine and only on API 33+
    override fun startLineHyphenEdit(line: Int): Int = NO_HYPHEN_EDIT

    override fun endLineHyphenEdit(line: Int): Int = NO_HYPHEN_EDIT

    /**
     * Returns the width of the text between [start] and [end], measured style run by style
     * run with each run's own paint. This matches how the text is drawn and how x positions
     * are advanced by the layout algorithm.
     */
    override fun width(start: Int, end: Int): Float {
        val from = start.coerceIn(0, spannable.length)
        val to = end.coerceIn(0, spannable.length)
        if (from >= to) return 0.0f

        var total = 0.0f
        var cursor = from
        for (run in styleRuns) {
            if (run.end <= cursor) continue
            if (run.start >= to) break

            val runStart = max(cursor, run.start)
            val runEnd = min(to, run.end)
            total += run.paint.measureText(spannable, runStart, runEnd)
            cursor = runEnd

            if (cursor >= to) break
        }
        return total
    }

    /**
     * Builds the `StaticLayout` used to compute the line breaks. The break strategy
     * constants expected by [StaticLayout.Builder.setBreakStrategy] are declared on
     * `android.graphics.text.LineBreaker` (API 29+), so passing the equivalent, identically
     * valued `Layout.BREAK_STRATEGY_HIGH_QUALITY` constant documented for this API triggers
     * a `WrongConstant` lint error even though the value is correct.
     */
    @SuppressLint("WrongConstant")
    private fun buildLayout(basePaint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder
            .obtain(spannable, 0, spannable.length, basePaint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .build()

    private class StyleRun(val start: Int, val end: Int, val paint: TextPaint)

    /**
     * Applies the full state of a style's paint onto the paint used by the layout to
     * measure text. Since the paint carries all the metric-affecting properties (typeface,
     * size, scaling, spacing, locales, etc.), a wholesale copy is both correct and simple.
     */
    private class StylePaintSpan(private val paint: TextPaint) : MetricAffectingSpan() {
        override fun updateMeasureState(textPaint: TextPaint) {
            textPaint.set(paint)
        }

        override fun updateDrawState(textPaint: TextPaint) {
            textPaint.set(paint)
        }
    }
}
