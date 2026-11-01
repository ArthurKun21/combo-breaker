package io.github.arthurkun21.text.combobreaker

import dev.romainguy.text.combobreaker.TextFlowHyphenation

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
 * the appropriate platform engine through [createMeasuredLines].
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
     * of bounds. The sign convention matches the platform line breaker's ascent values.
     */
    fun lineAscent(line: Int): Float

    /**
     * Returns the descent of the line at index [line], or 0.0f if [line] is out of bounds.
     */
    fun lineDescent(line: Int): Float

    /**
     * Returns the hyphen edit to apply at the start of the line at index [line]. Always
     * [NO_HYPHEN_EDIT] on platforms that do not compute hyphenation.
     */
    fun startLineHyphenEdit(line: Int): Int

    /**
     * Returns the hyphen edit to apply at the end of the line at index [line]. Always
     * [NO_HYPHEN_EDIT] on platforms that do not compute hyphenation.
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
