package io.github.arthurkun21.text.combobreaker

import androidx.compose.ui.text.TextStyle
import dev.romainguy.text.combobreaker.Interval
import dev.romainguy.text.combobreaker.TextFlowHyphenation

/**
 * Measures and breaks the remaining text of a paragraph at the specified width, using the
 * platform's line-breaking engine: `android.graphics.text.LineBreaker` +
 * `android.graphics.text.MeasuredText` on Android API 29 and above, a `StaticLayout`-based
 * fallback on Android API 24 to 28, and a Compose `TextMeasurer`-based engine on the JVM.
 *
 * The measured text always starts at [paragraphOffset]: styles must cover the entire
 * measured text, and the line breaker therefore expects to compute breaks over it.
 *
 * @param paragraph The text to measure, starting at [paragraphOffset] inside the paragraph.
 * @param styles The merged styles of the paragraph, relative to the start of the paragraph.
 * @param paragraphOffset The offset inside the paragraph at which the measured text starts.
 * @param paintForStyle Returns the paint to use to measure and draw the given style.
 * @param width The width available to break lines into.
 * @param hyphenation The hyphenation behavior to use.
 */
internal expect fun createMeasuredLines(
    paragraph: String,
    styles: List<Interval<TextStyle>>,
    paragraphOffset: Int,
    paintForStyle: (TextStyle) -> PlatformTextPaint,
    width: Float,
    hyphenation: TextFlowHyphenation,
): MeasuredLines
