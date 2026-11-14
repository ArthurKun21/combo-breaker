package io.github.arthurkun21.text.combobreaker

import androidx.compose.ui.graphics.Path

/**
 * Flattens this path into a list of `(t, x, y)` triples, one per point along the path,
 * sampling every contour. The `t` value is the cumulative arc length across the whole
 * path. The first point of each contour is emitted with the running arc length of the
 * previous contour, so that the segment joining two contours has equal `t` values on both
 * ends and is skipped by consumers (see [dev.romainguy.text.combobreaker.Path.toIntervals]).
 *
 * @param acceptableError The maximum error, in pixels, allowed by the flattening.
 */
internal expect fun Path.flatten(acceptableError: Float): FloatArray

/**
 * Expands this path by [margin], using the same stroke-and-fill geometry expansion as the
 * platform (a stroke of width `margin * 2` unioned with the path's fill), and returns the
 * expanded path. On platforms without direct access to stroke-to-fill geometry the result
 * may be a polygonal approximation of the expansion.
 *
 * @param path The path to expand. May be modified in place and returned.
 * @param margin The margin to add around the path.
 * @return The expanded path.
 */
internal expect fun inflatePath(path: Path, margin: Float): Path
