package io.github.arthurkun21.text.combobreaker

import android.os.Build
import android.text.TextPaint
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import dev.romainguy.text.combobreaker.createTextPaint
import dev.romainguy.text.combobreaker.lineHeight

/**
 * [PlatformTextPaint] implementation wrapping the platform's [TextPaint].
 */
internal class AndroidPlatformTextPaint(
    val paint: TextPaint,
) {
    /**
     * Returns the measured width of the text between the offsets [start] (inclusive) and
     * [end] (exclusive).
     */
    internal fun measureText(text: String, start: Int, end: Int): Float =
        paint.measureText(text, start, end)

    /**
     * Returns the height of a line of text, i.e. the distance between the ascent and the
     * descent of the font used by this paint.
     */
    internal val lineHeight: Float
        get() = paint.lineHeight

    /**
     * Configures this paint to draw the specified hyphen edits and word spacing during
     * subsequent text drawing. Only supported on API 29 and above.
     */
    internal fun setJustificationState(startHyphen: Int, endHyphen: Int, wordSpacing: Float) {
        if (Build.VERSION.SDK_INT >= 29) {
            paint.startHyphenEdit = startHyphen
            paint.endHyphenEdit = endHyphen
            paint.wordSpacing = wordSpacing
        }
    }
}

internal actual typealias PlatformTextPaint = AndroidPlatformTextPaint

internal actual fun createPlatformTextPaint(
    fontFamilyResolver: FontFamily.Resolver,
    style: TextStyle,
    density: Density,
): PlatformTextPaint = AndroidPlatformTextPaint(createTextPaint(fontFamilyResolver, style, density))

internal actual val supportsWordSpacing: Boolean
    get() = Build.VERSION.SDK_INT >= 29
