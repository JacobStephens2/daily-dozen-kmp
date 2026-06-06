# Daily Dozen — Compose Multiplatform

A local-first tracker for Dr. Greger's 12 Daily Dozen food categories, built as
a **Compose Multiplatform** app that shares both logic *and* UI across Android,
iOS, and the web (Wasm).

> Portfolio contrast: Daily Dozen shares logic **and** UI via Compose
> Multiplatform; Cascade shares only logic via a Rust core with native shells —
> two multiplatform architectures chosen to fit each app's needs.

## Download

Signed Android APKs are published on the
[Releases page](https://github.com/JacobStephens2/daily-dozen-kmp/releases).

Each release is **built and signed entirely in CI** from the tagged commit
([`.github/workflows/release.yml`](.github/workflows/release.yml)) and ships
with a `.sha256` checksum, so the published artifact can be verified against
the source it was built from:

```bash
sha256sum -c daily-dozen-<version>.apk.sha256
```

> Early preview (`v0.1.x`): a working local-first tracker on Android and web.
> Account sync against the existing Daily Dozen backend (see `SYNC_CONTRACT.md`)
> and the iOS build are still in progress.

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
    data/                       # SQLDelight repository (reactive Flows)
    di/                         # Koin wiring
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
| Persistence | SQLDelight 2.1.0 (first release with a wasmJs web-worker driver) |
| DI        | Koin 4.0 |
| Dates     | kotlinx-datetime |
| Build     | Gradle 8.14.3, AGP 8.7.3 |

`expect`/`actual` is reserved for the truly platform-specific: just the
SQLDelight driver and a date helper.

## Building

JDK 21 and the Android SDK are required. The committed Gradle config is
host-portable (it's what CI uses); point `ANDROID_HOME` at your SDK and build:

```bash
export ANDROID_HOME=$HOME/Android/Sdk

# Android
./gradlew :androidApp:assembleDebug

# Web / Wasm
./gradlew :composeApp:wasmJsBrowserDistribution
```

- **Android** and **Wasm** build on Linux; both are exercised on every push by
  [`.github/workflows/ci.yml`](.github/workflows/ci.yml).
- **iOS** can only be built on macOS with Xcode — see `iosApp/README.md`.

### Signed release builds

The release build is signed when a keystore is supplied — via environment
variables (how CI passes its secrets) or a gitignored `keystore.properties` at
the repo root for local builds:

```properties
storeFile=release.keystore
storePassword=…
keyAlias=…
keyPassword=…
```

```bash
./gradlew :androidApp:assembleRelease
```

With no keystore present the release build still succeeds, just unsigned — so a
fresh clone builds with no secrets. Tagging a commit `vX.Y.Z` and pushing the
tag triggers [`release.yml`](.github/workflows/release.yml), which builds the
signed APK in CI and attaches it (plus a checksum) to a GitHub Release.

## Status

A working **local-first tracker** on Android and web (Wasm): the 12 categories
render with tappable serving steppers, and daily state persists through
SQLDelight (reactive `Flow`s, Koin-injected) — verified end-to-end on Android
and in headless Chrome for Wasm.

In progress: account **sync** against the existing Daily Dozen backend (spec in
[`SYNC_CONTRACT.md`](SYNC_CONTRACT.md)), a history UI, and the **iOS** launcher
(Mac-only build).
