package io.github.arthurkun21.text.combobreaker

/**
 * Mutable two-dimensional point, providing the small subset of the platform's mutable point
 * API used by the text flow layout algorithms, in common code.
 */
internal class PointF(var x: Float = 0.0f, var y: Float = 0.0f) {
    /**
     * Sets the coordinates of this point.
     */
    fun set(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    /**
     * Copies the coordinates of [other] into this point.
     */
    fun set(other: PointF) {
        x = other.x
        y = other.y
    }
}
