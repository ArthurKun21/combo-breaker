package io.github.arthurkun21.text.combobreaker

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density

/**
 * Platform text paint used by the text flow layout algorithm to measure text and to carry
 * the state of a merged [TextStyle] to the platform drawing code.
 */
internal expect class PlatformTextPaint {
    /**
     * Returns the measured width of the text between the offsets [start] (inclusive) and
     * [end] (exclusive).
     */
    internal fun measureText(text: String, start: Int, end: Int): Float

    /**
     * Returns the height of a line of text, i.e. the distance between the ascent and the
     * descent of the font used by this paint.
     */
    internal val lineHeight: Float

    /**
     * Configures this paint to draw the specified hyphen edits and word spacing during
     * subsequent text drawing. Only honored on platforms with native word spacing support
     * (Android API 29+); other platforms emulate word spacing at draw time and ignore
     * this call.
     */
    internal fun setJustificationState(startHyphen: Int, endHyphen: Int, wordSpacing: Float)
}

/**
 * Creates a platform paint for the specified merged text [style], resolving typefaces
 * through [fontFamilyResolver].
 */
internal expect fun createPlatformTextPaint(
    fontFamilyResolver: FontFamily.Resolver,
    style: TextStyle,
    density: Density,
): PlatformTextPaint

/**
 * Returns true when the platform paint supports word spacing natively (Android API 29+).
 * When false, word spacing is emulated during layout and drawing, mirroring the behavior
 * of Android API levels below 29.
 */
internal expect val supportsWordSpacing: Boolean
