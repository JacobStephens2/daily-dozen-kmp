# iOS launcher

This folder holds the thin SwiftUI launcher for Daily Dozen. It hosts the
shared Compose UI via `MainViewControllerKt.MainViewController()` (defined in
`composeApp/src/iosMain`) — no business or UI logic lives here.

## Building

iOS can only be built on macOS with Xcode. The `.xcodeproj` is intentionally
**not** committed from this Linux dev box; generate it on a Mac:

1. Open this project in Android Studio with the Kotlin Multiplatform plugin, or
   use the KMP wizard, to generate `iosApp.xcodeproj` wired to the
   `:composeApp` framework (`embedAndSignAppleFrameworkForXcode`).
2. Set the framework search to the `ComposeApp` static framework produced by
   `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`.
3. Build & run from Xcode.

Files already in place:
- `iosApp/iOSApp.swift` — `@main` entry + `ComposeView` bridge.
- `iosApp/Info.plist` — bundle metadata.
