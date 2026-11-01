# Combo Breaker

This repository contains a Jetpack Compose library that flows text around arbitrary shapes over
multiple columns.

## Git Commit Policy

- **Never run `git commit` automatically.**
- **Never run `git commit --amend` under any circumstances**.
- If unsure whether a change should be part of an existing commit or a new one, **default to a new commit**.

## Modules

### `:combo-breaker`

The core library (Android, Jetpack Compose, no Material dependency). Package
`dev.romainguy.text.combobreaker`. Key sources:

- `BasicTextFlow.kt` — the layout composable, plus the public API types `FlowType`,
  `TextFlowJustification`, `TextFlowHyphenation`, `TextFlowLayoutResult`, and `TextFlowScope`
  (with the `flowShape` / `flowShapes` modifiers)
- `TextLayout.kt` — multi-column text layout pipeline
- `MeasuredLines.kt` / `StaticLayoutLines.kt` — internal line-breaking abstraction over the
  remaining text of a paragraph: `LineBreaker` + `MeasuredText` on API 29+, `StaticLayout`
  fallback on API 24–28
- `FlowShape.kt` — internal flow-shape representation: a `Path` contour flattened into segments
  and indexed as a vertical interval tree
- `IntervalTree.kt` — interval tree used for fast per-line queries
- `FlowSlots.kt` — `findFlowSlots()`, which finds the rectangular slots where text can be laid
  out around the flow shapes
- `Geometry.kt`, `Clipping.kt`, `TextPaint.kt`, `Utilities.kt` — geometry, clipping and paint helpers

### `:combo-breaker-material3`

Thin Material3 wrapper (package `dev.romainguy.text.combobreaker.material3`). `TextFlow.kt`
provides `String` and `AnnotatedString` overloads that read `LocalTextStyle` as the default style
and delegate everything to `BasicTextFlow`. Keep this module a thin wrapper — all layout logic
belongs in `:combo-breaker`.

### `:combo-breaker-demo`

Single-Activity demo app showing flow shapes, multiple columns, justification and hyphenation
(`ComboBreakerActivity.kt` + `ui/theme/`). Depends on `:combo-breaker-material3` and
`dev.romainguy:pathway`.

### `build-logic`

Included build providing the convention plugins every module applies. Keep module build files
thin — change build behavior in the convention plugins, not in the modules:

- `combo-breaker.android.library` / `combo-breaker.android.application`
- `combo-breaker.compose` — Compose compiler and runtime setup
- `combo-breaker.code.lint` — Spotless + ktlint
- `combo-breaker.dokka` — API documentation
- `combo-breaker.maven.publish` — Vanniktech Maven publish setup for Maven Central

## Build & Toolchain

- Compose dependencies come from `org.jetbrains.compose.*` coordinates (Compose Multiplatform),
  not `androidx.compose.*`
- All dependency versions live in `gradle/libs.versions.toml`; SDK and Java versions live only in
  `build-logic/convention/.../AndroidConfig.kt`
- Repositories are declared once in `settings.gradle.kts` (`FAIL_ON_PROJECT_REPOS`)
- Gradle configuration cache is enabled

## Workflow

```shell
./gradlew build          # assemble all modules
./gradlew spotlessCheck  # formatting/lint gate enforced in CI
./gradlew :combo-breaker:compileDebugKotlin :combo-breaker-material3:compileDebugKotlin
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
place them in `src/test/` (JVM) or `src/androidTest/` (instrumented/Compose UI) of the module
under test, mirroring its package structure.

## Domain Notes

- Every child of `TextFlow` / `BasicTextFlow` contributes a *flow shape* (a `Path`), attached via
  `Modifier.flowShape(...)` or `flowShapes(...)` with a `FlowType` and optional margin
- `FlowType` is bit-encoded internally: `OutsideLeft`/`OutsideRight`/`Outside` carry left/right
  flow bits, while `OutsideStart`/`OutsideEnd` resolve once against `LayoutDirection` and never
  appear in resolved layouts
- `FlowShape` flattens its path into segments and builds an interval tree over their vertical
  spans; `findFlowSlots()` queries that tree per line of text, reusing a `FlowSlotFinderState`
  to avoid per-line allocations
- Shape margins are implemented with `Path.op` unions, which is expensive on complex paths —
  see the README roadmap for known limitations (BiDi, ellipsize, margin performance)
- Text is line-broken by `android.graphics.text.LineBreaker` + `MeasuredText` on API 29+ and by a
  `StaticLayout`-based fallback (`MeasuredLines`/`StaticLayoutLines`) on API 24–28; justification
  relies on `Paint.setWordSpacing` on API 29+ and is emulated word-by-word at draw time below,
  which is why `minSdk` is 24
- Hyphenation only takes effect on API 33+, although `minSdk` is 24
