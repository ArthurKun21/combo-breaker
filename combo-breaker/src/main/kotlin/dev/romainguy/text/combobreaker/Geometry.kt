/*
 * Copyright (C) 2022 Romain Guy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.romainguy.text.combobreaker

import android.graphics.PathMeasure
import android.graphics.RectF
import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import android.graphics.Path as AndroidPath

internal class PathSegment(val x0: Float, val y0: Float, val x1: Float, val y1: Float)

/**
 * Returns an [IntervalTree] for this path. Each [Interval] in that tree wraps a path segment
 * generated from flattening the path. The interval itself is the vertical interval of a given
 * segment. The resulting interval tree can be used to quickly query which segments intersect
 * a given interval such as a line of a text.
 */
internal fun Path.toIntervals(
    intervals: IntervalTree<PathSegment> = IntervalTree(),
): IntervalTree<PathSegment> {
    intervals.clear()

    // An error of 1 px is enough for our purpose as we don't need to AA the path
    val pathData = if (Build.VERSION.SDK_INT >= 26) {
        asAndroidPath().approximate(1.0f)
    } else {
        // Path.approximate() requires API 26, fall back to a PathMeasure based
        // flattening on API 24 and 25
        asAndroidPath().approximateCompat(1.0f)
    }
    val pointCount = pathData.size / 3

    if (pointCount > 1) {
        for (i in 1 until pointCount) {
            val index = i * 3
            val prevIndex = (i - 1) * 3

            val d = pathData[index]
            val x = pathData[index + 1]
            val y = pathData[index + 2]

            val pd = pathData[prevIndex]
            val px = pathData[prevIndex + 1]
            val py = pathData[prevIndex + 2]

            if (d != pd && (x != px || y != py)) {
                val segment = PathSegment(px, py, x, y)
                val intervalStart = min(segment.y0, segment.y1)
                val intervalEnd = max(segment.y0, segment.y1)

                intervals += Interval(intervalStart, intervalEnd, segment)
            }
        }
    }

    return intervals
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun RectF.toOffset() = Offset(left, top)

@Suppress("NOTHING_TO_INLINE")
internal inline fun RectF.toSize() = Size(width(), height())

/**
 * Fallback for [AndroidPath.approximate], only available on API 26 and above. Flattens the
 * path into a list of `(t, x, y)` triples, one per point along the path, sampling every
 * contour with a [PathMeasure] at a fixed arc length step derived from [acceptableError].
 *
 * The `t` value is the cumulative arc length across the whole path. The first point of each
 * contour is emitted with the running arc length of the previous contour so that the segment
 * joining two contours has equal `t` values on both ends and is skipped by consumers (see
 * the segment filtering in [Path.toIntervals]).
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
