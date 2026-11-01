package io.github.arthurkun21.text.combobreaker

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * [PlatformTextPaint] implementation wrapping the merged [style] of a text run, measuring
 * text through a Compose [TextMeasurer] backed by the platform's font stack.
 */
internal class JvmPlatformTextPaint(
    val style: TextStyle,
    private val resolver: FontFamily.Resolver,
    density: Density,
) {
    /**
     * Measurer shared with the line-breaking engine to measure and draw text with this
     * paint's style.
     */
    internal val textMeasurer = TextMeasurer(resolver, density, LayoutDirection.Ltr)

    /**
     * Returns the measured width of the text between the offsets [start] (inclusive) and
     * [end] (exclusive).
     */
    internal fun measureText(text: String, start: Int, end: Int): Float {
        if (start >= end) return 0.0f

        return textMeasurer.measure(
            AnnotatedString(text.substring(start, end), style.toSpanStyle()),
            style,
            softWrap = false,
            maxLines = 1,
        ).size.width.toFloat()
    }

    /**
     * Returns the height of a line of text, i.e. the distance between the ascent and the
     * descent of the font used by this paint.
     */
    internal val lineHeight: Float by lazy {
        val layout = textMeasurer.measure(
            AnnotatedString("H", style.toSpanStyle()),
            style,
            softWrap = false,
            maxLines = 1,
        )
        layout.getLineBottom(0) - layout.getLineTop(0)
    }

    /**
     * Word spacing is not natively supported on this platform and is emulated at draw
     * time, so this call is a no-op.
     */
    internal fun setJustificationState(startHyphen: Int, endHyphen: Int, wordSpacing: Float) {
    }
}

internal actual typealias PlatformTextPaint = JvmPlatformTextPaint

internal actual fun createPlatformTextPaint(
    fontFamilyResolver: FontFamily.Resolver,
    style: TextStyle,
    density: Density,
): PlatformTextPaint = JvmPlatformTextPaint(style, fontFamilyResolver, density)

internal actual val supportsWordSpacing: Boolean = false
