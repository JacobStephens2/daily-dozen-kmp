# CLAUDE.md — Daily Dozen KMP

## Environment notes
- **GitHub CLI (`gh`) is authenticated** to the user's account on this machine.
  Use `gh` for all GitHub operations (clone, PRs, issues, API).
- User git email: `jstephens@vagabondtours.com`.

## Project
Compose Multiplatform app (shares logic **and** UI) targeting Android, iOS, and
web (Wasm). One shared `:composeApp` module; thin launchers per platform.
See `README.md` and `SESSION_STATE.md` for architecture and status.

## Building the iOS app — WORKS (verified in the iOS Simulator, 2026-05-29)
Built and launched on macOS with Xcode 26.5. The repo was scaffolded on Linux,
so a few things had to be set up / fixed; full recipe:

**Toolchain (this machine):**
- Xcode 26.5 at `/Applications/Xcode.app` (active via `xcode-select`).
- JDK: `/Users/admin/.jdks/jdk-21.0.11+10/Contents/Home` → set `JAVA_HOME`.
- Android SDK: `/Users/admin/Library/Android/sdk` → set `ANDROID_HOME`. Required
  because `:composeApp` applies the Android library plugin even for the iOS build.
- `local.properties` (gitignored) holds `sdk.dir=/Users/admin/Library/Android/sdk`.

**Steps:**
1. `JAVA_HOME=... ANDROID_HOME=... ./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`
2. The `iosApp.xcodeproj` is **generated** from `iosApp/project.yml` via XcodeGen
   (`xcodegen generate --spec iosApp/project.yml`) — not committed, by convention.
   A pre-build script runs `:composeApp:embedAndSignAppleFrameworkForXcode`.
3. `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator \
   -configuration Debug -destination 'generic/platform=iOS Simulator' \
   -derivedDataPath build/DerivedData ARCHS=arm64 ONLY_ACTIVE_ARCH=NO \
   CODE_SIGNING_ALLOWED=NO build`
4. Run: `xcrun simctl boot <udid>` → `simctl install <udid> <app>` →
   `simctl launch <udid> page.stephens.dailydozen`.

**Non-obvious fixes that were required (already applied to the working tree):**
- `gradle.properties`: removed a hardcoded Linux `-Djava.io.tmpdir=/mnt/...` that
  doesn't exist on macOS.
- **`ARCHS=arm64`** is mandatory: `generic/platform=iOS Simulator` otherwise builds
  universal (arm64+x86_64), but the K/N framework is only `iosSimulatorArm64`.
- **`-lsqlite3`** in `OTHER_LDFLAGS` (in `project.yml`): the static ComposeApp
  framework's SQLDelight `NativeSqliteDriver` needs the system SQLite.
- **`CADisableMinimumFrameDurationOnPhone=true`** in `Info.plist`: Compose MP's
  launch-time `PlistSanityCheck` throws without it.

## iOS Koin wiring (done)
- Koin is started at iOS launch: `composeApp/src/iosMain/.../di/KoinInitializer.kt`
  exposes `doInitKoin()` (→ `KoinInitializerKt.doInitKoin()` in Swift), called from
  `iOSApp.init()` in `iosApp/iosApp/iOSApp.swift`. This is the iOS counterpart to
  Android's `DailyDozenApp` and Wasm's `main()`. Without it `koinViewModel()` in
  `ChecklistScreen` crashes at runtime. ✅ Verified working at launch.
