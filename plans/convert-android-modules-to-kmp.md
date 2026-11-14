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

---

# Demo directory restructure + demo KMP conversion (Android + JVM)

## Context

The library modules (`:combo-breaker`, `:combo-breaker-material3`) are already KMP; the demo
is the last Android-only module. Goal: create a `demo/` directory, move `combo-breaker-demo`
into it, convert it to a KMP module (Android + JVM targets) holding the shared demo UI +
desktop entrypoint, and add a thin Android application module next to it as the Android
entrypoint (the AGP KMP android target is library-only — an APK needs a separate
`com.android.application` module).

- Reference: `ArthurKun21/generic-datastore` `samples/preferenceComposeApp` — single KMP
  sample module (`commonMain`/`androidMain`/`jvmMain`, `jvm()` target, desktop entry in
  `Main.kt`) set up via a dedicated sample convention plugin (`gd.kmp.sample`).
- Images move to **Compose Multiplatform resources**
  (`commonMain/composeResources/drawable/` + generated `Res.drawable.*` accessors). This
  requires the `org.jetbrains.compose` Gradle plugin (for accessor generation and
  `compose.desktop.application`) — a departure from the library modules, which use raw CMP
  coordinates only.
- Pathway publishes **only an Android AAR** (verified in its Gradle module metadata): its
  Apache-2.0 marching-squares tracer (`Image.kt` + `Contours.kt`, ~400–450 lines, operates
  on an `IntArray` of pixels) is **ported into the demo's commonMain**; both platforms trace
  identically and the pathway dependency is dropped.

## Step 1 — Restructure into `demo/` (all moves via `git mv`)

1. Delete the stale `demo/` contents (only gitignored build outputs + empty dirs from an
   earlier attempt), then `git mv combo-breaker-demo demo/combo-breaker-demo`.
2. Inside the moved module (git mv, Apache headers preserved):
   - `src/main/java/.../demo/ui/theme/{Color,Shape,Theme,Type}.kt` →
     `src/commonMain/kotlin/dev/romainguy/text/combobreaker/demo/ui/theme/`
   - `src/main/java/.../demo/ComboBreakerActivity.kt` →
     `demo/combo-breaker-demo-android/src/main/java/dev/romainguy/text/combobreaker/demo/`
     (edited down to a thin shell)
   - `src/main/res/drawable-xxhdpi/{microphone,badge,letter_t}.png`, `hearts.png`,
     `landscape.jpg` → `src/commonMain/composeResources/drawable/` (unqualified dir:
     deterministic full-res bytes on both platforms)
   - `mipmap-anydpi-v26/*`, `drawable/ic_launcher_*.xml`, `values/strings.xml`,
     `values/themes.xml`, `proguard-rules.pro` → `demo/combo-breaker-demo-android/`
   - Drop `drawable/baseline_line_style_24.xml` (unreferenced anywhere)
3. `settings.gradle.kts`: replace `include(":combo-breaker-demo")` with
   `include(":demo:combo-breaker-demo")` + `include(":demo:combo-breaker-demo-android")`.

## Step 2 — Build setup (mirrors the reference's convention-plugin structure)

**`gradle/libs.versions.toml`**: add
`compose-gradle-plugin = { module = "org.jetbrains.compose:compose-gradle-plugin", version.ref = "compose" }`
(libraries) and `jetbrains-compose = { id = "org.jetbrains.compose", version.ref = "compose" }`
(plugins); remove the now-unused `pathway` alias/version.

**`build-logic/convention/build.gradle.kts`**: add the compose-gradle-plugin classpath dep;
register two plugins:

- `combo-breaker.compose.multiplatform` → **`ComposeMultiplatformConventionPlugin`**:
  applies `combo-breaker.compose` + `org.jetbrains.compose`
- `combo-breaker.kotlin.multiplatform.sample` → **`KmpSampleConventionPlugin`** (modeled on
  the reference's `gd.kmp.sample` + existing `KmpLibraryConventionPlugin`): applies
  kotlin-multiplatform, android-kmp-library, compose-multiplatform, code.lint;
  `applyDefaultHierarchyTemplate()`; **no `explicitApi()`** (demo, not library); android
  target (`KotlinMultiplatformAndroidLibraryTarget`) gets compileSdk/minSdk from
  `AndroidConfig` and `androidResources.enable = true` (required for resources per the CMP
  docs); `jvm()` with JVM_21; `-Xexpect-actual-classes`

**Root `build.gradle.kts`**: `alias(libs.plugins.jetbrains.compose) apply false`.

## Step 3 — `demo/combo-breaker-demo` (KMP sample module)

**Build file**: plugin `combo-breaker.kotlin.multiplatform.sample`;
`kotlin { android { namespace = "dev.romainguy.text.combobreaker.demo.shared" } }` (distinct
from the app's namespace); `commonMain.dependencies { api(project(":combo-breaker-material3")); implementation(compose.components.resources) }`;
`jvmMain.dependencies { implementation(compose.desktop.currentOs) }`; and
`compose.desktop.application { mainClass = "io.github.arthurkun21.text.combobreaker.demo.MainKt" }`.

**commonMain**:

- `TextFlowDemo.kt` (package `dev.romainguy.text.combobreaker.demo`, derives from
  `ComboBreakerActivity.kt`): moved composables `TextFlowDemo`, `DemoControls`,
  `MaterialDemoSetup` (private → internal) plus a public `ComboBreakerDemoApp()` =
  `MaterialDemoSetup { TextFlowDemo() }` — the single entry both entrypoints call. Bitmap
  decoding blocks replaced by `rememberDemoAssets()`; the `/2` microphone downscale
  is dropped (trace at full res, same shape). Sample text, justification/hyphenation state,
  and `flowShape`/`flowShapes` usage unchanged.
- `ui/theme/` files unchanged except **`Theme.kt`**: the `Build.VERSION.SDK_INT >= S`
  dynamic-color branch becomes
  `(if (dynamicColor) dynamicColorScheme(darkTheme) else null) ?: if (darkTheme) dark else light`.
- New files in package `io.github.arthurkun21.text.combobreaker.demo`:
  - `ImageTracing.kt` — faithful port of pathway's marching squares (`Image.kt` +
    `Contours.kt`, Apache 2.0): `ImageBitmap.toPath(alphaThreshold, minAngle): Path` and
    `ImageBitmap.toPaths(): List<Path>`, reading pixels via the common
    `ImageBitmap.toPixelMap()` (fallback: tiny expect/actual pixel reader). No platform
    branches, no pathway.
  - `DemoAssets.kt` — `class DemoAsset(bitmap: ImageBitmap, shapes: List<Path>)`,
    `class DemoAssets(...)`, `@Composable fun rememberDemoAssets()`: decodes `Res.drawable.*`
    via the synchronous `org.jetbrains.compose.resources.imageResource` (the generated
    `Res.readBytes` is suspend, and the demo loads synchronously like the original);
    traces microphone (`alphaThreshold = 0.5f`, as today), badge (defaults) and hearts
    (`toPaths()`); letter_t/landscape stay plain bitmaps.
  - `DynamicColorScheme.kt` — `@Composable expect fun dynamicColorScheme(darkTheme: Boolean): ColorScheme?`

**androidMain**: `DynamicColorScheme.android.kt` — the `SDK_INT >= S` + `LocalContext` +
`dynamicDark/LightColorScheme` branch moved verbatim. No extra deps.

**jvmMain**: `Main.kt` — `fun main() = application { Window(onCloseRequest = ::exitApplication,
title = "Combo Breaker Demo", state = rememberWindowState(720.dp, 1280.dp)) { ComboBreakerDemoApp() } }`;
`DynamicColorScheme.jvm.kt` returns `null`.

## Step 4 — `demo/combo-breaker-demo-android` (Android entrypoint)

New app module via the existing `combo-breaker.android.application` +
`combo-breaker.compose` conventions (unchanged build logic): namespace/applicationId
`dev.romainguy.text.combobreaker.demo` (unchanged installed id), same release
buildType/packaging config as today; deps: `implementation(project(":demo:combo-breaker-demo"))`,
`libs.androidx.activity.compose`, `libs.androidx.core.ktx`,
`libs.androidx.lifecycle.runtime.ktx`. The moved `ComboBreakerActivity.kt` is trimmed to:
`enableEdgeToEdge()` + `setContent { ComboBreakerDemoApp() }`. Manifest (launcher activity)
unchanged; res = moved launcher icons + strings + themes.

## Step 5 — CI + docs

- `.github/workflows/ci.yml`: paths `combo-breaker-demo/**` → `demo/**`; build step →
  `./gradlew :demo:combo-breaker-demo-android:assembleRelease` (+ `:demo:combo-breaker-demo:jvmJar`).
- `AGENTS.md`: rewrite the demo module section — `:demo:combo-breaker-demo` (KMP sample,
  android+jvm, compose resources, desktop entrypoint via `compose.desktop.application`) +
  `:demo:combo-breaker-demo-android` (Android app entrypoint); note the two new convention
  plugins and that the pathway dependency left the demo.

## Out of scope

Desktop distribution/packaging config (dmg/msi/deb), iOS targets, pathway library changes,
JVM hyphenation/RTL parity (tracked separately).

## Verification

1. `./gradlew build spotlessCheck` — all modules, all targets compile; CI formatting gate passes.
2. `./gradlew :demo:combo-breaker-demo-android:assembleDebug` — Android APK entrypoint builds.
3. `./gradlew :demo:combo-breaker-demo:run` — desktop window launches, skiko natives load,
   shapes render (timeout-kill after confirming).
4. `git status` review — moves are history-preserving; scope limited to demo + settings +
   build-logic + CI + docs. No commits (per repo policy).
