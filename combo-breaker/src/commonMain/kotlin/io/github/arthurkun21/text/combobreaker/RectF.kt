package io.github.arthurkun21.text.combobreaker

/**
 * Mutable floating-point rectangle, providing the small subset of the platform's mutable
 * rectangle API used by the text flow layout algorithms, in common code.
 */
internal class RectF(
    var left: Float = 0.0f,
    var top: Float = 0.0f,
    var right: Float = 0.0f,
    var bottom: Float = 0.0f,
) {
    /**
     * Creates a rectangle copying the coordinates of [source].
     */
    constructor(source: RectF) : this(source.left, source.top, source.right, source.bottom)

    /**
     * Returns true if the rectangle is empty (`left >= right` or `top >= bottom`).
     */
    val isEmpty: Boolean
        get() = left >= right || top >= bottom

    /**
     * Sets the rectangle to the empty rectangle with all coordinates at 0.
     */
    fun setEmpty() {
        left = 0.0f
        top = 0.0f
        right = 0.0f
        bottom = 0.0f
    }

    /**
     * Returns the rectangle's width (`right - left`).
     */
    fun width(): Float = right - left

    /**
     * Returns the rectangle's height (`bottom - top`).
     */
    fun height(): Float = bottom - top

    /**
     * Translates the rectangle by [dx] and [dy].
     */
    fun offset(dx: Float, dy: Float) {
        left += dx
        top += dy
        right += dx
        bottom += dy
    }
}
