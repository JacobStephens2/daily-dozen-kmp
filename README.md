# Daily Dozen — Compose Multiplatform

A local-first tracker for Dr. Greger's 12 Daily Dozen food categories, built as
a **Compose Multiplatform** app that shares both logic *and* UI across Android,
iOS, and the web (Wasm).

> Portfolio contrast: Daily Dozen shares logic **and** UI via Compose
> Multiplatform; Cascade shares only logic via a Rust core with native shells —
> two multiplatform architectures chosen to fit each app's needs.

## Architecture

One shared `composeApp` module holds 90%+ of the code; three thin launchers
just boot the shared `App()` composable.

```
composeApp/src/
  commonMain/kotlin/page/stephens/dailydozen/
    App.kt                      # root composable the launchers call
    domain/                     # pure logic — no Compose, no platform deps
      model/DozenCategory.kt
      model/CategoryProgress.kt
      DozenCatalog.kt           # the canonical 12 categories + targets
    ui/
      checklist/ChecklistScreen.kt, ChecklistViewModel.kt
      components/DozenRow.kt, ServingStepper.kt
      theme/Theme.kt
    data/                       # (SQLDelight repository — next milestone)
    di/                         # (Koin wiring — next milestone)
  androidMain/                  # Android-only actuals
  iosMain/MainViewController.kt # iOS framework entry
  wasmJsMain/main.kt            # web (Wasm) entry → ComposeViewport { App() }

androidApp/                     # thin Android launcher (MainActivity → App())
iosApp/                         # thin SwiftUI launcher (hosts App via UIViewController)
```

## Stack

| Concern   | Choice |
|-----------|--------|
| UI        | Compose Multiplatform 1.7.3 (Material 3) |
| Language  | Kotlin 2.0.21 |
| State     | `lifecycle-viewmodel-compose` (multiplatform) |
| Persistence | SQLDelight 2.0.2 *(next milestone — mature Wasm support)* |
| DI        | Koin 4.0 *(next milestone)* |
| Dates     | kotlinx-datetime |
| Build     | Gradle 8.14.3, AGP 8.7.3 |

`expect`/`actual` is reserved for the truly platform-specific: just the
SQLDelight driver and a date helper.

## Building

This repo was scaffolded on a Linux box. The Gradle home is kept on a data
volume to spare the root disk:

```bash
export GRADLE_USER_HOME=/mnt/volume_nyc3_01/jacob/.gradle
export ANDROID_HOME=/home/jacob/Android/Sdk

# Android (verified build target on Linux)
./gradlew :androidApp:assembleDebug

# Web / Wasm
./gradlew :composeApp:wasmJsBrowserDistribution
```

- **Android** and **Wasm** build on this Linux box.
- **iOS** can only be built on macOS with Xcode — see `iosApp/README.md`.

## Status

**Milestone 1 — compiling skeleton:** all three targets declared, three
launchers in place, `App.kt → ChecklistScreen.kt` renders the 12 categories
with tappable serving steppers from an in-memory catalog.

Next: SQLDelight persistence (daily state + read-only history), then Koin DI.
