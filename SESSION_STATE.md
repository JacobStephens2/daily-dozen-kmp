# Session state — Daily Dozen KMP (paused for server RAM upgrade + reboot)

_Saved 2026-05-29. Resume from here._

## What this is
Fresh **Compose Multiplatform** rebuild of Daily Dozen (shares logic AND UI
across Android / iOS / web-Wasm). Separate from the old vanilla-JS PWA at
`/var/www/dailydozen.stephens.page`. Portfolio contrast piece vs. Cascade.

## Where everything lives
- **Project root:** `/mnt/volume_nyc3_01/jacob/dailydozen-kmp` (on the 100 GB data volume, NOT root disk)
- **Gradle home:** `/mnt/volume_nyc3_01/jacob/.gradle`  → always `export GRADLE_USER_HOME=/mnt/volume_nyc3_01/jacob/.gradle`
- **Android SDK:** `/home/jacob/Android/Sdk` → `export ANDROID_HOME=/home/jacob/Android/Sdk`
- **JDK:** system Java 21
- **JVM temp:** forced to `/mnt/volume_nyc3_01/jacob/tmp` via `org.gradle.jvmargs` in `gradle.properties` (see below)

## Versions (chosen to match what's already cached in the Gradle home)
Gradle 8.14.3 · Kotlin 2.0.21 · Compose Multiplatform 1.7.3 · AGP 8.7.3 ·
kotlinx-datetime 0.6.1 · (deferred: SQLDelight 2.0.2, Koin 4.0.0)

## STATUS — Milestone 1 (compiling skeleton) is essentially DONE
- All three targets declared in `composeApp/build.gradle.kts` (androidTarget, ios{X64,Arm64,SimulatorArm64}, wasmJs{browser}).
- Three launchers in place: `androidApp/` (MainActivity→App()), `iosApp/` (SwiftUI, Mac-only build — see iosApp/README.md), `composeApp/src/wasmJsMain/main.kt` (ComposeViewport{App()}).
- Shared UI: `App.kt → ui/checklist/ChecklistScreen.kt` renders the 12 categories
  from `domain/DozenCatalog.kt` with tappable `ServingStepper`s + a ViewModel.
- **The Android app COMPILED and produced a 9 MB `androidApp-debug.apk`** on attempt 3
  (timestamp 19:26). So the code + Gradle config are good.

## Fixes already applied during this session
1. Plugin classpath conflict (kotlin.android vs kotlin.multiplatform on shared
   classpath) → added `alias(libs.plugins.kotlinAndroid) apply false` to root `build.gradle.kts`. RESOLVED.
2. `@Preview` unresolved in App.kt → added `implementation(compose.components.uiToolingPreview)`
   to commonMain in `composeApp/build.gradle.kts`. RESOLVED.
3. Build temp filled `/tmp` (root disk) → added `-Djava.io.tmpdir=/mnt/volume_nyc3_01/jacob/tmp`
   to `org.gradle.jvmargs` in `gradle.properties`. APPLIED, needs a verifying rebuild.

## THE BLOCKER right now: root disk 100% full
`df -h /` → 48G/48G, ~136K free. The last clean build failed ONLY with
`No space left on device` in `:androidApp:mergeDebugJavaResource` — a disk
issue, NOT a code/config issue. Root-fs consumers found:
`/home/jacob/.local` 3.2G, `.cache` 2.1G (incl `.cache/ms-playwright` 1.3G),
`.rustup` 2.0G, `.bubblewrap` 2.0G, `Android` 1.1G. Reclaimable junk in `/tmp`:
`node_modules` 47M, `puppeteer_dev_chrome_profile-*` 26M, `node-compile-cache`
26M, old `kotlin-daemon.*.log`. (User may also be resizing the disk.)

## NEXT STEPS after reboot (in order)
1. Free root-disk space (or confirm volume covers it). With tmpdir now on the
   volume, the build may already pass — but root fs still needs headroom.
2. Verify Android: `cd <root> && export GRADLE_USER_HOME=... ANDROID_HOME=... && ./gradlew :androidApp:assembleDebug`
   → expect BUILD SUCCESSFUL + apk at `androidApp/build/outputs/apk/debug/`.
3. Verify Wasm: `./gradlew :composeApp:wasmJsBrowserDistribution` (Task #3).
4. Then Task #4: wire SQLDelight (db schema, expect/actual driver per platform,
   repository for daily state + read-only history). Then Koin DI. iOS = Mac-only.

## Gotchas learned
- Don't `pkill -f "gradlew ..."` — it matches the harness's own background
  wrapper and kills the build (saw exit 144).
- Two Kotlin/Native iOS targets auto-disable on Linux (expected warning, fine).
- AGP 8.7.3 > Kotlin's max-tested 8.5 → benign warning.
