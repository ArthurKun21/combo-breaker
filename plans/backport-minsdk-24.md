# Backport Combo Breaker to minSdk 24

## Context

`:combo-breaker` and `:combo-breaker-material3` currently require API 29 (`AndroidConfig.MIN_SDK = 29`), but the README roadmap already lists "Backport to earlier API levels" as a goal. This backport targets minSdk **24**.

The API-29 dependency is not `StaticLayout` — the whole engine is built on `android.graphics.text.LineBreaker` + `MeasuredText` (both API 29, unguarded) plus `Paint.setWordSpacing`/`setStartHyphenEdit`/`setEndHyphenEdit` (API 29) and `Path.approximate` (API 26). Everything else is already ≤ 24 or correctly gated (hyphenation ≥ 33, dynamic color ≥ 31, `setTextLocales` ≥ 24).

**Verified dependency floor is 23** (from AAR manifests in the Gradle cache): Compose UI 1.12.x, activity-compose 1.13, core-ktx 1.19, lifecycle 2.11 → 23; pathway → 21. So minSdk 24 is safe; lower is impossible with the current dependency set.

**Decisions:** minSdk = 24; justification on API 24–28 is **emulated word-by-word** (no silent degradation).

## Approach: two line-breaking engines behind an internal abstraction

API 24–28 gets a `StaticLayout.Builder`-based fallback engine (API 23 — unconditionally available at minSdk 24) producing the same per-line data the API 29+ `LineBreaker` engine produces today. Public API is unchanged (explicit API mode untouched; everything new is `internal`).

### Step 1 — Build config (1 line)
- `build-logic/convention/src/main/kotlin/combobreaker/buildlogic/AndroidConfig.kt:9`: `MIN_SDK = 29` → `24`. This propagates to all modules incl. the demo (dynamic color and `res/mipmap-anydpi-v26` already degrade correctly).

### Step 2 — New internal engine abstraction (2 new files in `:combo-breaker`, package `dev.romainguy.text.combobreaker`)

**`MeasuredLines.kt`** — interface + factory + API 29+ implementation:

```kotlin
internal interface MeasuredLines {
    fun lineCount(): Int
    fun lineBreakOffset(line: Int): Int      // end of line, relative to measured text
    fun lineWidth(line: Int): Float
    fun lineAscent(line: Int): Float         // negative, LineBreaker convention
    fun lineDescent(line: Int): Float
    fun startLineHyphenEdit(line: Int): Int  // 0 below 33
    fun endLineHyphenEdit(line: Int): Int
    fun width(start: Int, end: Int): Float   // offsets relative to measured text
}
```

- `MeasuredLines.Empty` object (lineCount 0, all zeros) to replace the dummy `MeasuredText` in `TextLayoutState`.
- Factory `computeMeasuredLines(state, width, hyphenation): MeasuredLines` — `Build.VERSION.SDK_INT >= 29` → `LineBreakerLines`, else `StaticLayoutLines`.
- `LineBreakerLines` (`@RequiresApi(29)`): wraps the exact code moved out of `TextLayout.kt:98-103,184-193` — `LineBreaker.Builder` (HIGH_QUALITY, HYPHENATION_FREQUENCY_FULL, JUSTIFICATION_MODE_NONE), `MeasuredText.Builder` + style runs via the existing `appendStyleRuns`/`MeasuredTextHelper.hyphenation` logic; `width()` → `MeasuredText.getWidth`. Behavior on 29+ is byte-identical to today.
- Class-loading safety: `LineBreaker`/`MeasuredText` types must only be reachable from code guarded by the SDK check (mirror the existing `MeasuredTextHelper.kt` pattern). `TextLayoutState` holds only the `MeasuredLines` interface type.

**`StaticLayoutLines.kt`** — API 24–28 engine:

- Build a `SpannableString` over the paragraph substring (same `charArray` semantics as `MeasuredText.Builder` today: remaining text from `paragraphOffset`), attach one custom `MetricAffectingSpan` per merged style interval (same filtering as `appendStyleRuns`, `TextLayout.kt:341-354`) whose `updateMeasureState`/`updateDrawState` copy the cached style paint via `Paint.set(paintForStyle(style))`.
- `StaticLayout.Builder.obtain(spannable, 0, len, basePaint, ceil(width))` with `ALIGN_NORMAL`, `setIncludePad(false)`, `setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)`, `setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)` — hyphenation stays API 33+ (documented behavior).
- Mapping: `lineBreakOffset` = `layout.getLineEnd(line)`; `lineWidth` = measured via own `width()` over `[lineStart, trimEndSpace(lineEnd)]` (excludes trailing whitespace, self-consistent with `justify()`); `lineAscent/Descent` = `layout.getLineAscent/getLineDescent`; hyphen edits = 0.
- `width(start, end)` = sum of `paint.measureText(spannable, s, e)` per intersecting style run (robust even across a style boundary).
- Guard `line >= lineCount` by treating it as end-of-paragraph (breakOffset = measured length) to keep the consumption loop in `TextLayout.kt` terminating.

### Step 3 — Refactor `TextLayout.kt` to the abstraction
- Delete `lineBreaker`/`constraints` creation (`:98-103`) — moves into `LineBreakerLines`; `constraints.width` at `:172` becomes the width passed to the factory.
- `TextLayoutState`: replace `measuredText`/`result` (`:516-519`) with `var lines: MeasuredLines = MeasuredLines.Empty`.
- Rebuild block (`:177-197`) and per-line queries (`:203-210`) go through `state.lines`.
- `:286-289` → `state.lines.width(start - state.paragraphOffset, end - state.paragraphOffset)`.
- x-advance branch (`:277-291`): on API ≥ 29 keep exactly as-is (`wordSpacing` affects `measureText`). Below 29: skip the `paint.startHyphenEdit/endHyphenEdit/wordSpacing` writes (always `NO_EDIT` below 33) and compute `x += paint.measureText(paragraph, start, end) + justifyWidth * countStretchableSpaces(paragraph, start, end)`.
- `justify()` `:406`: change to `state.lines.width(paragraphLastLineOffset - state.paragraphOffset, endOffset - state.paragraphOffset)`. This fixes a pre-existing offset bug (paragraph-absolute offsets passed where measured-relative are expected — on 29+ `MeasuredText.getWidth` silently measured a shifted window; `measureText` would throw out-of-bounds). Flag in the commit message; slightly more correct shrink on 29+.
- `hyphenation()` extension (`:656-663`) and `MeasuredTextHelper.kt` unchanged.

### Step 4 — Draw path in `BasicTextFlow.kt` (`:443-449`)
Gate the API 29 paint properties; below 29 emulate word spacing by drawing word-by-word:

```kotlin
if (Build.VERSION.SDK_INT >= 29) { ...existing code... } else { drawPreApi29(c, segment) }
```

`drawPreApi29` (private helper in `BasicTextFlow.kt`): if `justifyWidth == 0f` → plain `drawText`; else draw each maximal non-U+0020 run at an advancing x, advancing per space char by `measureText(" ") + justifyWidth` (negative values shrink "desperate" lines, matching minikin's U+0020-only word-spacing semantics and `countStretchableSpaces`).

### Step 5 — `Geometry.kt:41` — `Path.approximate` (API 26) fallback
`if (Build.VERSION.SDK_INT >= 26) asAndroidPath().approximate(1.0f) else asAndroidPath().flattenCompat(1.0f)` — private `flattenCompat(acceptableError)` using `PathMeasure` (API 1): iterate contours via `nextContour()`, sample `getPosTan` at a fixed arc-length step (~2–4 px), emit `(t, x, y)` triples matching `approximate`'s format (consumed at `Geometry.kt:42-64`), include exact contour endpoints, never join points across contours (strictly more correct than `approximate` at contour jumps).

### Step 6 — Docs
- `README.md:29`: "Compatible with API 29+" → "Compatible with API 24+"; add one line noting hyphenation requires API 33+ and justification is emulated below API 29; remove roadmap item `README.md:175` ("Backport to earlier API levels").
- `AGENTS.md`: update "minSdk is 29" note (→ 24) and document the two engines + word-spacing emulation in Domain Notes.

### Not changing
`:combo-breaker-material3` (pure Compose wrapper), demo code (dynamic color already gated), `TextPaint.kt` (locale gate already correct for 24), public API surface.

## Verification
1. `./gradlew build` — compiles all modules; **lint runs as part of `check` and will flag any remaining unguarded `NewApi` call** (the static gate that the abstraction is airtight).
2. `./gradlew spotlessCheck` (CI gate; run `spotlessApply` if needed).
3. Runtime on an **API 24 or 25 emulator** (demo app): text flows around shapes, multiple columns, justification (visually compare justified columns), RTL layout, AnnotatedString styling; `TextFlowJustification.Auto` + `TextFlowHyphenation.Auto` combos.
4. Regression on an API 33+ emulator: hyphenation still works, justified output pixel-equivalent to current behavior (the engine swap must not change 29+ output).
5. Commit on the current branch `feat/backport-api-version` (Conventional Commits: `feat: backport text flow to API 24`, `docs: update supported API levels`); PR targets `development`.

## Risks
- **StaticLayout metric fidelity** (ascent/descent conventions, trailing-whitespace in widths, `includePad`) is the highest-uncertainty area — that's why 4) compares against 33+ output. Tuning knobs, in order: use `layout.getLineWidth(line)` instead of own measure; include/exclude trailing spaces in `lineWidth`; fall back to per-line `TextPaint.getFontMetricsInt` over the line's style paints for ascent/descent.
- Perf: `StaticLayout` rebuilds are heavier than `LineBreaker.computeLineBreaks`, but the existing `lastSlotWidth` caching still avoids re-measures per slot; acceptable for a legacy-device path.
- Optional follow-up (not in scope): enable hyphenation on API 29–32 via the deprecated `MeasuredText.Builder.setComputeHyphenation(boolean)` (API 29).
