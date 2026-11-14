package io.github.arthurkun21.text.combobreaker

import androidx.compose.ui.graphics.drawscope.DrawScope
import dev.romainguy.text.combobreaker.TextSegment

/**
 * Draws a single [TextSegment] computed by the text flow layout algorithm at the segment's
 * position, applying the segment's hyphen edits and justification width using the best
 * mechanism available on the platform.
 */
internal expect fun DrawScope.drawTextSegment(segment: TextSegment)
