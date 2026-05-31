package page.stephens.dailydozen.di

/**
 * iOS startup hook for the SwiftUI launcher. Mirrors what `DailyDozenApp`
 * (Android) and `main()` (Wasm) do: start Koin exactly once before any
 * `koinViewModel()` is resolved. iOS needs no extra context — the native
 * SQLDelight driver is built without one — so this just calls [initKoin].
 *
 * Exposed as `KoinInitializerKt.doInitKoin()` to Swift.
 */
fun doInitKoin() = initKoin()
