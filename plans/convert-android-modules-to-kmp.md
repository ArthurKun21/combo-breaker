# Convert `:combo-breaker` and `:combo-breaker-material3` to KMP (Android + JVM)

## Context

Both library modules are Android-only. Goal: convert them to Kotlin Multiplatform with
`jvm()` and Android targets, using expect/actual for platform-specific code. The demo app
is NOT converted (later work). The reference is `KmpLibraryConventionPlugin.kt` from
generic-datastore (applies `kotlin-multiplatform` + `android-library` + lint; configures
`explicitApi()`, default hierarchy template, `KotlinMultiplatformAndroidLibraryTarget`
compileSdk/minSdk, `jvm()`, and `-Xexpect-actual-classes`).

Favorable starting points:

- Compose deps already use CMP coordinates (`org.jetbrains.compose.ui:ui` 1.12.2,
  `org.jetbrains.compose.material3:material3` 1.9.0) — KMP-aware artifacts that resolve
  per-target from `commonMain` without the `org.jetbrains.compose` plugin
- `MeasuredLines` (`io.github.arthurkun21.text.combobreaker`) is already a platform-neutral
  line-breaking abstraction
- CMP ≥1.9 text APIs (`TextMeasurer`, `MultiParagraph`) are multiplatform → the JVM engine
  needs no new dependencies
- AGENTS.md conventions already split upstream (`dev.romainguy...`) vs new files
  (`io.github.arthurkun21...`)

## Scope policy

- **File moves**: every tracked file move/rename uses `git mv` (preserves history);
  newly created files are just written normally (untracked).
- **Narrowly scoped**: no code reformats, no renames, no signature changes beyond what the
  platform split requires, no behavioral changes on Android. Android behavior today
  (incl. the API-level branching) is preserved as-is in `androidMain`.

## Step 1 — Build setup

**`gradle/libs.versions.toml`**

- `[libraries]`: add
  `kotlin-gradle-plugin = { module = "org.jetbrains.kotlin:kotlin-gradle-plugin", version.ref = "kotlin" }`
- `[plugins]`: add
  `kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }`
- No `org.jetbrains.compose` plugin needed — existing CMP coordinates resolve per-target
  via Gradle metadata; fallback if a target fails to resolve: apply `org.jetbrains.compose`

**`build-logic/convention/build.gradle.kts`**: add `implementation(libs.kotlin.gradle.plugin)`;
register id `combo-breaker.kotlin.multiplatform` → `KmpLibraryConventionPlugin`

**New `build-logic/convention/src/main/kotlin/KmpLibraryConventionPlugin.kt`** (modeled on
the reference, adapted):

```kotlin
with(pluginManager) {
    apply(libs.pluginId("kotlin-multiplatform"))
    apply(libs.pluginId("android-library"))
    apply("combo-breaker.code.lint")
}
extensions.configure<KotlinMultiplatformExtension> {
    explicitApi()
    applyDefaultHierarchyTemplate()
    targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
        compileSdk = AndroidConfig.COMPILE_SDK
        minSdk = AndroidConfig.MIN_SDK
    }
    jvm()
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21             // match AndroidConfig.JavaVersion
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
```

Also set `LibraryExtension` compileOptions to Java 21 (mirrors
`AndroidLibraryConventionPlugin`). No `buildFeatures.compose` (KGP compose compiler plugin
handles it).

**Root `build.gradle.kts`**: add `alias(libs.plugins.kotlin.multiplatform) apply false`

**Module build files** (`combo-breaker`, `combo-breaker-material3`): replace
`combo-breaker.android.library` with `combo-breaker.kotlin.multiplatform`; keep
`combo-breaker.compose`, `combo-breaker.dokka`, `combo-breaker.maven.publish`; drop the
`kotlin { explicitApi() }` block (now in the convention); keep `android { namespace = ... }`;
move deps into `kotlin { sourceSets { commonMain.dependencies { ... } } }` (same
api/implementation roles); delete `src/main/AndroidManifest.xml` (AGP 9 doesn't require it);
drop `POM_PACKAGING=aar` from module `gradle.properties` (vanniktech handles KMP packaging).

## Step 2 — `:combo-breaker` source restructuring

New source sets: `commonMain`, `androidMain`, `jvmMain`. Moves (all via `git mv`,
`mkdir -p` first):

| Current (`src/main/kotlin/...`) | Destination |
|---|---|
| `dev/romainguy/.../BasicTextFlow.kt` | `commonMain/kotlin/dev/romainguy/.../` |
| `dev/romainguy/.../TextLayout.kt` | `commonMain/kotlin/dev/romainguy/.../` |
| `dev/romainguy/.../FlowSlots.kt` | `commonMain/kotlin/dev/romainguy/.../` |
| `dev/romainguy/.../Clipping.kt` | `commonMain/kotlin/dev/romainguy/.../` |
| `dev/romainguy/.../Geometry.kt` | `commonMain/kotlin/dev/romainguy/.../` (common part) |
| `dev/romainguy/.../FlowShape.kt` | `commonMain/kotlin/dev/romainguy/.../` (unchanged) |
| `dev/romainguy/.../IntervalTree.kt` | `commonMain/kotlin/dev/romainguy/.../` (unchanged) |
| `dev/romainguy/.../Utilities.kt` | `commonMain/kotlin/dev/romainguy/.../` (unchanged) |
| `io/github/arthurkun21/.../MeasuredLines.kt` | `commonMain/kotlin/io/github/arthurkun21/.../` (keep interface + `NO_HYPHEN_EDIT` + `Empty`; move `LineBreakerLines`/helpers out to androidMain) |
| `dev/romainguy/.../TextPaint.kt` | `androidMain/kotlin/dev/romainguy/.../` |
| `dev/romainguy/.../Locale.kt` | `androidMain/kotlin/dev/romainguy/.../` |
| `dev/romainguy/.../MeasuredTextHelper.kt` | `androidMain/kotlin/dev/romainguy/.../` |
| `io/github/arthurkun21/.../StaticLayoutLines.kt` | `androidMain/kotlin/io/github/arthurkun21/.../` |

### New common files (package `io.github.arthurkun21.text.combobreaker`)

- `PointF.kt`, `RectF.kt` — common mutable geometry structs replicating only the used
  android.graphics surface (`x/y`, `set()`, `left/top/right/bottom` vars, `setEmpty()`,
  copy ctor, `width()/height()`, `offset()`). Consumed by Clipping/FlowSlots/TextLayout/
  debug overlay. (`IntervalTree.kt`, `Utilities.kt`, `FlowShape.kt` need no changes.)
- `PlatformTextPaint.kt` — `internal expect class PlatformTextPaint`:
  - `fun measureText(text: String, start: Int, end: Int): Float`
  - `val lineHeight: Float`
  - `fun setJustificationState(startHyphen: Int, endHyphen: Int, wordSpacing: Float)`
    (Android 29+: sets paint fields as today; JVM: no-op)
  - `expect fun createPlatformTextPaint(resolver, style, density): PlatformTextPaint`
  - `expect val supportsWordSpacing: Boolean` (Android: `SDK_INT >= 29`; JVM: `false` →
    reuse the existing <API 29 emulation path)
- `PlatformMeasuredLines.kt` —
  `expect fun createMeasuredLines(paragraph, styles, paragraphOffset, paintForStyle, width, hyphenation): MeasuredLines`
- `PlatformDrawing.kt` — `expect fun DrawScope.drawTextSegment(segment: TextSegment)`
- `PlatformGeometry.kt` — `expect fun Path.flatten(acceptableError: Float): FloatArray`,
  `expect fun inflatePath(path: Path, margin: Float): Path`

### Minimal edits in moved common files (platform split only)

- `TextSegment.paint: TextPaint` → `PlatformTextPaint`; `paints` cache →
  `Map<TextStyle, PlatformTextPaint>`; `paintForStyle` calls `createPlatformTextPaint`
- `Build.VERSION` branches (`TextLayout.kt:256/340`, `BasicTextFlow.kt:445`) replaced by
  `supportsWordSpacing` / expect calls; `computeMeasuredLines` delegates to
  `createMeasuredLines`
- `expandAndClipPath` → common: `inflatePath(path, margin)` + multiplatform
  `Path.op(clip, PathOperation.Intersect)`
- draw loop calls `drawTextSegment(segment)`; debug overlay uses
  `PlatformTextPaint.lineHeight`
- `Geometry.kt`: keep `toIntervals`/`PathSegment`/`toOffset`/`toSize` in common, calling
  `Path.flatten`; the API-26 `approximate`/`approximateCompat` code moves to the android
  actual

### New actuals

**`androidMain` (io.github.arthurkun21 package)**:

- `PlatformTextPaint.android.kt` — wraps android `TextPaint`, reusing the moved
  `TextPaint.kt` mapping
- `PlatformMeasuredLines.android.kt` — SDK dispatch → `LineBreakerLines`/`StaticLayoutLines`
- `PlatformDrawing.android.kt` — SDK 29+ `nativeCanvas.drawText` + pre-29 word-by-word
  emulation, moved verbatim from `BasicTextFlow.kt`
- `PlatformGeometry.android.kt` — `approximate`/`approximateCompat` via `asAndroidPath()`

**`jvmMain` (new files, io.github.arthurkun21 package)**:

- `PlatformTextPaint.jvm.kt` — wraps resolved compose `TextStyle` + `FontFamily.Resolver`
  typeface; measures via `TextMeasurer`
- `PlatformMeasuredLines.jvm.kt` — `TextMeasurerLines: MeasuredLines` backed by
  `TextMeasurer.measure()`/`MultiParagraph`: `lineBreakOffset` =
  `getLineEnd(line, visibleEnd=false)`, `lineWidth` = `getLineRight - getLineLeft`,
  `lineAscent/Descent` derived from `getLineBaseline/getLineTop/getLineBottom`;
  `width(start,end)` via horizontal positions (LTR assumption documented); hyphen edits
  always `NO_HYPHEN_EDIT`
- `PlatformDrawing.jvm.kt` — draws segments via `TextMeasurer` +
  `DrawScope.drawText(TextLayoutResult)`, emulating word spacing word-by-word (reuses the
  pre-29 algorithm shape)
- `PlatformGeometry.jvm.kt` — flattening via skiko `PathMeasure` (port of
  `approximateCompat`) through `asSkiaPath()`; margin inflation via skiko
  `Paint.getFillPath` (stroke-and-fill, `strokeWidth = margin * 2`)

## Step 3 — `:combo-breaker-material3`

`TextFlow.kt` is already pure multiplatform compose (only `LocalTextStyle` + state
primitives): `git mv` to `src/commonMain/kotlin/dev/romainguy/text/combobreaker/material3/`
unchanged, KMP build file per Step 1, no actuals needed. Thin-wrapper rule preserved.

## Step 4 — Docs (minimal, factual updates only)

- `AGENTS.md`: module descriptions → KMP android+jvm; mention the new
  `combo-breaker.kotlin.multiplatform` convention plugin
- `README.md`: note desktop/JVM support
- (Optional, on request) CI: add a `jvmJar` task to `.github/workflows/ci.yml`

## Out of scope

Demo module conversion, iOS/native targets, JVM hyphenation parity (JVM behaves like
Android <API 33 initially), JVM RTL/BiDi guarantees, any refactor not required by the
platform split.

## Verification

1. `./gradlew :combo-breaker:build :combo-breaker-material3:build` — all targets compile
   (common/android/jvm)
2. `./gradlew spotlessCheck` — must pass (CI gate)
3. `./gradlew :combo-breaker-demo:assembleDebug` — demo still consumes the converted
   modules' android target unchanged
4. `git status`/`git diff --stat` review — confirm moves are history-preserving and no
   unrelated changes slipped in
