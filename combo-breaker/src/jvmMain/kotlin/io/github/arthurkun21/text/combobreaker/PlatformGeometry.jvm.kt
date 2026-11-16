package io.github.arthurkun21.text.combobreaker

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathIterator
import androidx.compose.ui.graphics.PathSegment
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.PathMeasure
import org.jetbrains.skia.PathUtils
import kotlin.math.ceil
import kotlin.math.max
import org.jetbrains.skia.Path as SkiaPath

/**
 * Converts this compose path to its Skia equivalent by replaying its verbs into a
 * [PathBuilder]. Compose does not expose direct Skia path interop, so the conversion
 * walks the path segment by segment.
 */
private fun Path.toSkiaPath(): SkiaPath {
    val builder = PathBuilder()
    val iterator = PathIterator(this)
    val points = FloatArray(8)

    while (iterator.hasNext()) {
        when (val type = iterator.next(points, 0)) {
            PathSegment.Type.Move -> builder.moveTo(points[0], points[1])

            PathSegment.Type.Line -> builder.lineTo(points[2], points[3])

            PathSegment.Type.Quadratic -> builder.quadTo(points[2], points[3], points[4], points[5])

            PathSegment.Type.Conic -> builder.conicTo(points[2], points[3], points[4], points[5], points[6])

            PathSegment.Type.Cubic -> builder.cubicTo(
                points[2],
                points[3],
                points[4],
                points[5],
                points[6],
                points[7],
            )

            PathSegment.Type.Close -> builder.closePath()

            PathSegment.Type.Done -> {}
        }
    }

    return builder.detach()
}

internal actual fun Path.flatten(acceptableError: Float): FloatArray {
    val measure = PathMeasure(toSkiaPath(), false)
    val points = ArrayList<Float>()
    var arcLength = 0.0f

    do {
        val length = measure.length
        if (length > 0.0f) {
            val step = acceptableError * 4.0f
            val count = max(1, ceil(length / step).toInt())

            var pos = measure.getPosition(0.0f)!!
            points += arcLength
            points += pos.x
            points += pos.y

            for (i in 1 until count) {
                pos = measure.getPosition(i * length / count)!!
                points += arcLength + i * length / count
                points += pos.x
                points += pos.y
            }

            pos = measure.getPosition(length)!!
            arcLength += length
            points += arcLength
            points += pos.x
            points += pos.y
        }
    } while (measure.nextContour())

    return points.toFloatArray()
}

internal actual fun inflatePath(path: Path, margin: Float): Path {
    if (margin <= 0.0f) return path

    // Expand the path with the same stroke-and-fill geometry expansion used on Android:
    // a stroke of width margin * 2 unioned with the path's fill
    val expanded = PathUtils.fillPathWithPaint(
        path.toSkiaPath(),
        Paint().apply {
            mode = PaintMode.STROKE_AND_FILL
            strokeWidth = margin * 2.0f
        },
    )

    // Sample the expanded geometry into a compose path. The expanded path is flattened
    // into segments as soon as it is turned into a FlowShape, so a polyline
    // representation is equivalent for layout purposes.
    val measure = PathMeasure(expanded, false)
    val result = Path()

    do {
        val length = measure.length
        if (length > 0.0f) {
            val step = 1.0f
            val count = max(1, ceil(length / step).toInt())
            var first = true

            for (i in 0..count) {
                val pos = measure.getPosition(i * length / count)!!
                if (first) {
                    result.moveTo(pos.x, pos.y)
                    first = false
                } else {
                    result.lineTo(pos.x, pos.y)
                }
            }
        }
    } while (measure.nextContour())

    return result
}
