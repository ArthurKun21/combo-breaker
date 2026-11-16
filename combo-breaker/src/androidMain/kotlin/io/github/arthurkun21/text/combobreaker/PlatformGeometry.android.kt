package io.github.arthurkun21.text.combobreaker

import android.graphics.Paint
import android.graphics.PathMeasure
import android.os.Build
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import kotlin.math.ceil
import kotlin.math.max
import android.graphics.Path as AndroidPath

internal actual fun Path.flatten(acceptableError: Float): FloatArray {
    // Path.approximate() requires API 26, fall back to a PathMeasure based
    // flattening on API 24 and 25
    return if (Build.VERSION.SDK_INT >= 26) {
        asAndroidPath().approximate(acceptableError)
    } else {
        asAndroidPath().approximateCompat(acceptableError)
    }
}

internal actual fun inflatePath(path: Path, margin: Float): Path {
    if (margin > 0.0f) {
        val androidPath = path.asAndroidPath()
        Paint().apply {
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = margin * 2.0f
        }.getFillPath(androidPath, androidPath)
    }

    return path
}

/**
 * Fallback for [AndroidPath.approximate], only available on API 26 and above. Flattens the
 * path into a list of `(t, x, y)` triples, one per point along the path, sampling every
 * contour with a [PathMeasure] at a fixed arc length step derived from [acceptableError].
 *
 * The `t` value is the cumulative arc length across the whole path. The first point of each
 * contour is emitted with the running arc length of the previous contour so that the segment
 * joining two contours has equal `t` values on both ends and is skipped by consumers (see
 * the segment filtering in [dev.romainguy.text.combobreaker.Path.toIntervals]).
 */
private fun AndroidPath.approximateCompat(acceptableError: Float): FloatArray {
    val measure = PathMeasure(this, false)
    val points = ArrayList<Float>()
    val pos = FloatArray(2)
    var arcLength = 0.0f

    do {
        val length = measure.length
        if (length > 0.0f) {
            val step = acceptableError * 4.0f
            val count = max(1, ceil(length / step).toInt())

            measure.getPosTan(0.0f, pos, null)
            points += arcLength
            points += pos[0]
            points += pos[1]

            for (i in 1 until count) {
                measure.getPosTan(i * length / count, pos, null)
                points += arcLength + i * length / count
                points += pos[0]
                points += pos[1]
            }

            measure.getPosTan(length, pos, null)
            arcLength += length
            points += arcLength
            points += pos[0]
            points += pos[1]
        }
    } while (measure.nextContour())

    return points.toFloatArray()
}
