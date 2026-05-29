# Session state — Daily Dozen KMP

_Last updated 2026-05-29 (after the RAM-upgrade reboot + SQLDelight/Koin work)._

## What this is
Fresh **Compose Multiplatform** rebuild of Daily Dozen (shares logic AND UI
across Android / iOS / web-Wasm). Separate from the old vanilla-JS PWA at
`/var/www/dailydozen.stephens.page`. Portfolio contrast piece vs. Cascade.

## Where everything lives
- **Project root:** `/mnt/volume_nyc3_01/jacob/dailydozen-kmp` (100 GB data volume, NOT root disk)
- **Now a git repo** (`git init` done this session; 3 commits). `user.email` = jstephens@vagabondtours.com.
- **Gradle home:** `export GRADLE_USER_HOME=/mnt/volume_nyc3_01/jacob/.gradle`
- **Android SDK:** `export ANDROID_HOME=/home/jacob/Android/Sdk`
- **JDK:** system Java 21 · **JVM temp** forced to the volume via `gradle.properties` (root disk still ~98% full)

## Versions
Gradle 8.14.3 · Kotlin 2.0.21 · Compose MP 1.7.3 · AGP 8.7.3 ·
kotlinx-datetime 0.6.1 · **SQLDelight 2.1.0** (bumped from 2.0.2 — first version
with a wasmJs web-worker driver; built w/ Kotlin 2.0.20, klib-compatible) ·
**Koin 4.0.0**.

## STATUS — all originally-tracked milestones DONE
1. **Compiling skeleton** — done. Android APK builds; Wasm distribution builds.
2. **Verify Android** — `:androidApp:assembleDebug` BUILD SUCCESSFUL (10 MB APK).
3. **Verify Wasm** — `:composeApp:wasmJsBrowserDistribution` BUILD SUCCESSFUL.
4. **SQLDelight persistence + Koin DI** — done on all three targets:
   - Schema `db/DailyDozen.sq`: `servingLog(day, categoryId, count)` PK(day,category),
     `INSERT OR REPLACE` (baseline SQLite dialect, safe on minSdk 26).
   - `generateAsync=true` → one generated API drives both the synchronous native
     drivers (Android/iOS, adapted via `Schema.synchronous()` from async-extensions)
     and the async web-worker driver (Wasm).
   - `DatabaseDriverFactory` expect/actual: Android = AndroidSqliteDriver,
     iOS = NativeSqliteDriver, Wasm = WebWorkerDriver (sql.js worker).
   - `DozenRepository`: lazy DB open behind a Mutex; reactive `countsForDay()` +
     read-only `history()` Flows; `setCount()` writer.
   - `ChecklistViewModel` reads/writes through the repo (today via kotlinx-datetime),
     state via `stateIn`. UI gets the VM via `koinViewModel()`.
   - Koin: shared `appModule` + `expect val platformModule`; `initKoin()` started
     from `DailyDozenApp` (Android), `main()` (Wasm), and (TODO) iOS launcher.

## Runtime verification done this session
- **Wasm proven end-to-end** in headless Chrome (puppeteer-core + /usr/bin/google-chrome):
  app mounts, the sql.js **web worker spawns** (873.js), zero runtime errors, and
  tapping a category's "+" makes the count persist through SQLite and re-render
  (0 → 2 on Beans). Round-trip write→DB→read→UI confirmed.
- Test harness was throwaway in `/tmp/ddtest` (removed to reclaim root disk).

## NEXT STEPS / open work
- **iOS**: code is in place (NativeSqliteDriver, PlatformModule.ios) but is
  **Mac-only to build**; `iosApp/iOSApp.swift` still needs to call an iOS
  `initKoin()` at launch. Not verifiable on this Linux box.
- **Wasm persistence caveat**: the @cashapp sql.js worker keeps the DB **in-memory
  for the session** — it does NOT survive a full page reload (no IndexedDB
  persistence layer). Android/iOS persist to disk. Document this / add IndexedDB
  persistence if web durability is wanted.
- **History UI**: `DozenRepository.history()` exists but no screen consumes it yet.
- Possible polish: app icon, theming, week/streak view, tests.

## Gotchas learned
- Default SQLite dialect (3.18) has no UPSERT (`ON CONFLICT DO UPDATE`, needs 3.24
  / Android API 30+). Use `INSERT OR REPLACE` for minSdk 26.
- `INTEGER AS Int` forces a column adapter; plain `INTEGER` (Long) + `.toInt()` at
  the repo boundary is simpler.
- `Schema.synchronous()` lives in `app.cash.sqldelight:async-extensions`
  (package `app.cash.sqldelight.async.coroutines`), NOT the base runtime.
- SQLDelight web-worker driver needs npm `sql.js` + `@cashapp/sqldelight-sqljs-worker`
  + `copy-webpack-plugin`; `webpack.config.d/sqljs.js` copies `sql-wasm.wasm` and
  stubs Node core modules (`fs/path/crypto`) for the browser build.
- New npm deps change `kotlin-js-store/yarn.lock` → run `./gradlew kotlinUpgradeYarnLock`.
- Don't `pkill -f "gradlew ..."` — it matches the harness's background wrapper.
- Two Kotlin/Native iOS targets auto-disable on Linux (expected). AGP 8.7.3 > Kotlin's
  max-tested → benign warning. expect/actual-class Beta warning silenced via
  `-Xexpect-actual-classes`.
