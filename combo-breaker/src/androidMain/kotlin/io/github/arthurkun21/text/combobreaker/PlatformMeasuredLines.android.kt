package io.github.arthurkun21.text.combobreaker

import android.os.Build
import androidx.compose.ui.text.TextStyle
import dev.romainguy.text.combobreaker.Interval
import dev.romainguy.text.combobreaker.TextFlowHyphenation

/**
 * Selects the platform line-breaking engine: `android.graphics.text.LineBreaker` +
 * `android.graphics.text.MeasuredText` on API 29 and above, and a `StaticLayout`-based
 * fallback on API 24 to 28.
 */
internal actual fun createMeasuredLines(
    paragraph: String,
    styles: List<Interval<TextStyle>>,
    paragraphOffset: Int,
    paintForStyle: (TextStyle) -> PlatformTextPaint,
    width: Float,
    hyphenation: TextFlowHyphenation,
): MeasuredLines {
    return if (Build.VERSION.SDK_INT >= 29) {
        LineBreakerLines(
            paragraph.toCharArray(paragraphOffset),
            styles,
            paragraphOffset,
            paintForStyle,
            width,
            hyphenation,
        )
    } else {
        StaticLayoutLines(
            paragraph.substring(paragraphOffset),
            styles,
            paragraphOffset,
            paintForStyle,
            width,
        )
    }
}
