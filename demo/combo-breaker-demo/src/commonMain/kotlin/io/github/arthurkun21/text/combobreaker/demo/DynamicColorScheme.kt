package io.github.arthurkun21.text.combobreaker.demo

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/**
 * Returns the platform's dynamic color scheme when supported (Android 12+ on Android),
 * or `null` when dynamic colors are unavailable, in which case callers fall back to their
 * static color schemes.
 */
@Composable
expect fun dynamicColorScheme(darkTheme: Boolean): ColorScheme?
