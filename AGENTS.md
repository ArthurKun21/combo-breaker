# Combo Breaker

This repository contains a Jetpack Compose library that flows text around arbitrary shapes over
multiple columns.

## Git Commit Policy

- **Never run `git commit` automatically.**
- **Never run `git commit --amend` under any circumstances**.
- If unsure whether a change should be part of an existing commit or a new one, **default to a new commit**.

## Modules

### `:combo-breaker`

The core library (Kotlin Multiplatform: Android + JVM desktop, Jetpack Compose, no Material
dependency). Package `dev.romainguy.text.combobreaker` (commonMain unless noted). Key sources:

- `BasicTextFlow.kt` — the layout composable, plus the public API types `FlowType`,
  `TextFlowJustification`, `TextFlowHyphenation`, `TextFlowLayoutResult`, and `TextFlowScope`
  (with the `flowShape` / `flowShapes` modifiers)
- `TextLayout.kt` — multi-column text layout pipeline
- `MeasuredLines.kt` — internal line-breaking abstraction over the remaining text of a
  paragraph, in common code; platform engines are selected by the `createMeasuredLines`
  expect function (`io.github.arthurkun21.text.combobreaker`): `LineBreakerLines`
  (`LineBreaker` + `MeasuredText`) and `StaticLayoutLines` (`StaticLayout` fallback) in
  androidMain, `TextMeasurerLines` (Compose `TextMeasurer`) in jvmMain
- `PlatformTextPaint.kt` — internal expect class wrapping the platform paint used to measure
  text and draw segments (android `TextPaint` on Android, compose `TextStyle` + `TextMeasurer`
  on the JVM); `PointF.kt` / `RectF.kt` — common mutable geometry primitives
- `FlowShape.kt` — internal flow-shape representation: a `Path` contour flattened into segments
  and indexed as a vertical interval tree
- `IntervalTree.kt` — interval tree used for fast per-line queries
- `FlowSlots.kt` — `findFlowSlots()`, which finds the rectangular slots where text can be laid
  out around the flow shapes
- `Geometry.kt`, `Clipping.kt`, `Utilities.kt` — geometry, clipping and utility helpers;
  `TextPaint.kt`, `Locale.kt`, `MeasuredTextHelper.kt` — Android-only helpers in androidMain

### `:combo-breaker-material3`

Thin Material3 wrapper (package `dev.romainguy.text.combobreaker.material3`, entirely in
commonMain — it is pure Compose). `TextFlow.kt`
provides `String` and `AnnotatedString` overloads that read `LocalTextStyle` as the default style
and delegate everything to `BasicTextFlow`. Keep this module a thin wrapper — all layout logic
belongs in `:combo-breaker`.

### `:combo-breaker-demo`

Single-Activity Android demo app showing flow shapes, multiple columns, justification and
hyphenation (`ComboBreakerActivity.kt` + `ui/theme/`). Depends on `:combo-breaker-material3`
(its Android target) and `dev.romainguy:pathway`. Not yet converted to KMP.

### `build-logic`

Included build providing the convention plugins every module applies. Keep module build files
thin — change build behavior in the convention plugins, not in the modules:

- `combo-breaker.kotlin.multiplatform` — KMP setup for the libraries: applies
  `org.jetbrains.kotlin.multiplatform` + `com.android.kotlin.multiplatform.library`, configures
  `explicitApi()`, the android target (compileSdk/minSdk/jvmTarget from `AndroidConfig`), the
  `jvm()` target, and `-Xexpect-actual-classes`
- `combo-breaker.android.library` / `combo-breaker.android.application` — Android-only modules
  (the demo)
- `combo-breaker.compose` — Compose compiler and runtime setup
- `combo-breaker.code.lint` — Spotless + ktlint
- `combo-breaker.dokka` — API documentation
- `combo-breaker.maven.publish` — Vanniktech Maven publish setup for Maven Central

## Build & Toolchain

- Compose dependencies come from `org.jetbrains.compose.*` coordinates (Compose Multiplatform),
  not `androidx.compose.*`; the CMP artifacts resolve per-target from `commonMain` without the
  `org.jetbrains.compose` Gradle plugin
- The library modules are Kotlin Multiplatform (targets: `android`, `jvm`); the android target
  uses AGP's `com.android.kotlin.multiplatform.library` plugin and is configured in the module
  build files via `kotlin { android { ... } }` (namespace) and the convention plugin
  (compileSdk/minSdk/jvmTarget)
- All dependency versions live in `gradle/libs.versions.toml`; SDK and Java versions live only in
  `build-logic/convention/.../AndroidConfig.kt`
- Repositories are declared once in `settings.gradle.kts` (`FAIL_ON_PROJECT_REPOS`)
- Gradle configuration cache is enabled

## Workflow

```shell
./gradlew build          # assemble all modules, all targets
./gradlew spotlessCheck  # formatting/lint gate enforced in CI
./gradlew :combo-breaker:compileKotlinJvm :combo-breaker:assembleAndroidMain
./gradlew :combo-breaker-material3:compileKotlinJvm
./gradlew :combo-breaker-demo:compileDebugKotlin
```

- Always use the Gradle wrapper; Gradle Kotlin DSL everywhere
- Pull requests target the `development` branch
- Commit messages follow Conventional Commits (`feat:`, `fix:`, `chore(deps):`, `ci:`, `docs:`)
- Dependency updates are automated by Renovate — don't hand-bump versions while a Renovate PR is open

## Code Style

- Spotless with ktlint enforces formatting; `.editorconfig` is the source of style truth
  (120-character lines, trailing commas allowed, ktlint `intellij_idea` style)
- Both library modules use Kotlin **explicit API mode**: every public declaration must be marked
  `public` explicitly and carry KDoc; prefer `internal` for helpers
- Files are named after their primary declaration; original upstream sources carry the Apache 2.0
  copyright header — preserve it when editing those files
- New files are placed in the `io.github.arthurkun21.text.combobreaker` package; original upstream
  sources stay in `dev.romainguy.text.combobreaker`
- This is a layout library: state is handled with Compose primitives (`mutableStateOf`,
  `remember`, `derivedStateOf`, `@Immutable`/`@Stable`); no ViewModels, coroutines or Flow in the core

## Testing and Quality Assurance

There is currently no test suite; CI only verifies the build and formatting. Run `./gradlew build`
and `./gradlew spotlessCheck` before opening a PR — `spotlessCheck` must pass. If you add tests,
place them in the matching KMP source set (`commonTest` for platform-independent logic,
`jvmTest` for JVM, `androidDeviceTest`/`androidHostTest` for the AGP KMP android target),
mirroring the package structure.

## Domain Notes

- Every child of `TextFlow` / `BasicTextFlow` contributes a *flow shape* (a `Path`), attached via
  `Modifier.flowShape(...)` or `flowShapes(...)` with a `FlowType` and optional margin
- `FlowType` is bit-encoded internally: `OutsideLeft`/`OutsideRight`/`Outside` carry left/right
  flow bits, while `OutsideStart`/`OutsideEnd` resolve once against `LayoutDirection` and never
  appear in resolved layouts
- `FlowShape` flattens its path into segments and builds an interval tree over their vertical
  spans; `findFlowSlots()` queries that tree per line of text, reusing a `FlowSlotFinderState`
  to avoid per-line allocations
- Shape margins are implemented with stroke-and-fill path expansion (`getFillPath` on Android,
  Skia's `PathUtils.fillPathWithPaint` on the JVM) followed by a `Path.op` intersection, which
  is expensive on complex paths — see the README roadmap for known limitations (BiDi,
  ellipsize, margin performance)
- Text is line-broken per platform through `createMeasuredLines`: `android.graphics.text.LineBreaker`
  + `MeasuredText` on API 29+, a `StaticLayout`-based fallback (`LineBreakerLines`/
  `StaticLayoutLines`) on API 24–28, and a Compose `TextMeasurer`-based engine
  (`TextMeasurerLines`) on the JVM; justification relies on `Paint.setWordSpacing` on API 29+
  and is emulated word-by-word at draw time below and on the JVM, which is why `minSdk` is 24
- Hyphenation only takes effect on API 33+ on Android (the JVM engine does not compute
  hyphenation yet), although `minSdk` is 24
