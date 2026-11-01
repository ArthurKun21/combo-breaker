package io.github.arthurkun21.text.combobreaker.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import org.jetbrains.compose.resources.imageResource

/**
 * A single demo image and, when the image encodes one or more shapes, the contours traced
 * from it with [toPath]/[toPaths].
 */
class DemoAsset(
    val bitmap: ImageBitmap,
    val shapes: List<Path> = emptyList(),
)

/**
 * All the images used by the demo UI, decoded from the shared Compose Multiplatform resources.
 */
class DemoAssets(
    val microphone: DemoAsset,
    val badge: DemoAsset,
    val letterT: ImageBitmap,
    val landscape: ImageBitmap,
    val hearts: DemoAsset,
)

/**
 * Decodes and traces all the demo images, mirroring the original Android demo, which decoded
 * and traced its shapes synchronously at composition time.
 */
@Composable
fun rememberDemoAssets(): DemoAssets {
    val microphone = imageResource(Res.drawable.microphone)
    val badge = imageResource(Res.drawable.badge)
    val letterT = imageResource(Res.drawable.letter_t)
    val landscape = imageResource(Res.drawable.landscape)
    val hearts = imageResource(Res.drawable.hearts)

    return remember(microphone, badge, letterT, landscape, hearts) {
        DemoAssets(
            microphone = DemoAsset(microphone, listOf(microphone.toPath(alphaThreshold = 0.5f))),
            badge = DemoAsset(badge, listOf(badge.toPath())),
            letterT = letterT,
            landscape = landscape,
            hearts = DemoAsset(hearts, hearts.toPaths()),
        )
    }
}
