# Council: GPT-5.5 — Daily Dozen sign-in and sync implementation plan

## Executive recommendation

Build account sign-in and sync as a **small, safety-first compatibility layer over the existing opaque-blob backend**, not as a new sync platform. The authoritative contract says the backend is `https://dailydozen.stephens.page/api`, stores each user’s state as one opaque JSON payload, uses 30-day HS256 bearer JWTs, accepts `GET /data` and `PUT /data`, rate-limits auth flows, caps PUT bodies at 2 MB, and expects token storage in iOS Keychain, Android EncryptedSharedPreferences/Keystore, and Wasm `localStorage` [SYNC_CONTRACT.md](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/SYNC_CONTRACT.md). The KMP app currently has a local-first Compose Multiplatform tracker with SQLDelight persistence and Koin DI, while account sync and iOS launcher completion remain in progress [README.md](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/README.md). The right implementation objective is therefore: **byte-compatible web sync, no silent local destruction, reusable KMP auth/storage primitives, and enough verification rigor to make the work legible to a reviewer without pretending this is a platform**.

The main deviation I recommend from the web contract is deliberately narrow: keep the **server wire contract LWW-compatible**, but place a client-side safe-merge guard before any pull overwrites an existing device. The contract states web sync is whole-blob last-writer-wins and warns not to invent a smarter merge that diverges from the web [SYNC_CONTRACT.md](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/SYNC_CONTRACT.md). However, the product requirement here is stronger than parity: “no silent data loss, ever,” based on the Creighton tracker failure mode. Because `blob[dateKey][categoryId]` is a set of checked indices, a per-profile/per-day/per-category **union merge** is lossless for normal check additions and remains web-compatible because the resulting payload is still the exact contract shape [SYNC_CONTRACT.md](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/SYNC_CONTRACT.md). Recommendation: fresh device with no meaningful local data adopts server; existing device with unsynced local data never blind-pulls over local; it computes a safe union, preserves unknown JSON fields, surfaces a “merged server + this device” banner, and pushes the merged blob after explicit or clearly visible confirmation.

This should be scoped as a 4–5 milestone project, not a broad product relaunch. The work earns its keep only where it demonstrates reusable infrastructure: offline-first conflict handling, secure multiplatform token storage, Ktor client reliability, and data-dignity UX. Anything beyond that is scope creep.

## Phased plan with smallest-safe-step ordering and atomic commits

### Milestone 0 — Contract lock and safety harness

**Goal:** make data loss mechanically hard before adding login UI. Atomic commits: (0.1) add `sync-contract-vectors` tests for date keys, categories, presets, and §9 round-trip JSON fixtures; (0.2) add `BlobPayload` serialization models with unknown-field retention strategy; (0.3) add merge-property tests; (0.4) add ADR-0001 through ADR-0004 skeletons. Exit criteria: all §6 date vectors pass; JSON fixture from `SYNC_CONTRACT.md` decodes, re-encodes, and preserves unknown top-level/profile/category fields; merge is idempotent, commutative, associative for checked-index sets; and no production code path can call “replace local with remote” without returning a `SyncDecision` object first. The current schema stores `(day, categoryId, count)` with ISO date strings and `INSERT OR REPLACE`, so it cannot represent sparse checked indices, diet profiles, or unknown server fields [Bountywell.sq](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/composeApp/src/commonMain/sqldelight/page/stephens/bountywell/db/Bountywell.sq).

### Milestone 1 — Local persistence migration to contract-shaped data

**Goal:** replace count-based storage with contract-shaped JSON while preserving current local logs. Atomic commits: (1.1) create SQLDelight table `sync_blob(id TEXT PRIMARY KEY, payload TEXT NOT NULL, localRevision INTEGER, lastSyncedUpdatedAt TEXT, hasUnsyncedChanges INTEGER)` or equivalent single-row store; (1.2) migrate old `servingLog` rows by expanding count `N` into indices `[0, …, N-1]`; (1.3) replace repository APIs from count writes to set writes while keeping UI stepper semantics; (1.4) update catalog to 13 exact hyphenated IDs including `protein`, with 5 presets and `dietType/customServings`; (1.5) switch date boundary to JS `toDateString()` local-time keys. Exit criteria: old installs retain visible counts after migration; repository can export the exact payload; history reads group by JS date keys; and web-compatible IDs replace current underscores such as `other_fruits`, `other_veg`, `flaxseeds`, `nuts_seeds`, and `whole_grains` [CategoryCatalog.kt](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/composeApp/src/commonMain/kotlin/page/stephens/bountywell/domain/CategoryCatalog.kt).

### Milestone 2 — Auth substrate and secure token storage

**Goal:** create reusable KMP primitives without entangling them with Daily Dozen UI. Atomic commits: (2.1) add Ktor + kotlinx.serialization dependencies; (2.2) define `AuthApi`, `TokenStore`, `AuthSessionRepository`, and `Clock`-injectable refresh policy; (2.3) implement Android encrypted storage with Keystore-backed EncryptedSharedPreferences where available and plaintext fallback forbidden in release; (2.4) implement iOS Keychain actual with stable service/access-group naming; (2.5) implement Wasm `localStorage` actual with explicit “browser storage is not hardware-secure” warning; (2.6) add 401/refresh-loop tests. Tiny signatures only: `expect class TokenStore { suspend fun read(): StoredToken?; suspend fun write(token: StoredToken); suspend fun clearAuthOnly() }` and `class AuthClient(private val api: AuthApi, private val store: TokenStore)`. Exit criteria: token refresh occurs only when online and within the 7-day expiry window; a 401 triggers one refresh attempt, retries the original request once, then enters “signed out for network/auth only” state without deleting local data; and logout clears tokens only unless the user separately chooses “delete local data.”

### Milestone 3 — Sync engine and account lifecycle UX

**Goal:** ship register/login/logout/forgot-reset/export/delete with non-destructive sync semantics. Atomic commits: (3.1) implement `SyncEngine.syncOnLogin()` fresh-device vs existing-device decision; (3.2) implement 3-second debounced push after local writes; (3.3) implement 429/offline/2 MB error surfaces; (3.4) add login/register/reset screens; (3.5) add logout prompt: “keep data on this device / remove local data after export”; (3.6) add export JSON and account-deletion request flow if backend lacks deletion endpoint; (3.7) add “last synced / pending changes / conflict merged” transparency. Exit criteria: login on a fresh install adopts server; login on a device with local data produces an explicit merge/adopt choice; local edits offline stay visible and push later; 429 never loops; payload-size failure provides export instructions; and no auth state transition deletes the local blob.

### Milestone 4 — Platform launchers, CI, and release hardening

**Goal:** make it reproducible and demonstrable. Atomic commits: (4.1) expose iOS `initKoin()` bridge and call it from `iOSApp.swift`; (4.2) add Android unit tests and Wasm browser tests to CI; (4.3) add a macOS workflow or documented manual Mac gate for iOS; (4.4) add static secret scan and dependency audit; (4.5) add an emulator UI smoke test using `uiautomator dump` rather than headless screenshots, because the known environment can black-frame screenshots. Exit criteria: Linux CI still builds Android and Wasm as today [ci.yml](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/.github/workflows/ci.yml); iOS has a Mac-only checklist; release artifacts remain signed/checksummed in CI [release.yml](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/.github/workflows/release.yml); and the verification matrix below is green.

## ADR-style decision records

### ADR-0001 — Auth method

**Decision:** use email/password JWT as baseline. The backend already exposes `register`, `login`, `refresh-token`, `forgot-password`, and `reset-password`, and reset email infra is already implied by the canonical `APP_URL` behavior [SYNC_CONTRACT.md](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/SYNC_CONTRACT.md). **Runner-up:** magic link using Resend, because it reduces password handling and may be friendlier on mobile. **Rejected for now:** Sign in with Apple/Google, because social auth adds native SDK/config risk; if Google or another third-party/social login is added to iOS, Apple App Review Guideline 4.8 requires an equivalent privacy-preserving login option, and Sign in with Apple is the usual way to satisfy that requirement [Apple App Review Guidelines](https://developer.apple.com/app-store/review/guidelines/). Rationale: reuse infrastructure, avoid new cloud/certs, and spend complexity budget on data safety.

### ADR-0002 — Secure token storage and auth failure behavior

**Decision:** implement `TokenStore` as expect/actual: iOS Keychain, Android encrypted preferences or Keystore-backed storage, and Wasm `localStorage` with frank security labeling. **Runner-up:** store JWT in SQLDelight alongside the blob for simplicity. Rejected because it weakens token isolation and makes logout/data-retention semantics muddy. Behavior rule: “auth loss is not data loss.” Logout clears auth only by default; refresh failure preserves local data; Keychain/Keystore clearing leaves the app in signed-out/pending-local state; and 401 refresh can run once per request, never recursively.

### ADR-0003 — Sync model

**Decision:** wire-compatible LWW with a safe local-merge guard. Fresh device: pull server and set `lastSyncedUpdatedAt`. Existing device with local unsynced or non-empty data: GET server, compute `union(local, server)` over `profiles[*].data[dateKey][categoryId]`, retain unknown fields, then prompt/surface merge before PUT. **Runner-up:** plain web LWW. Rejected as insufficient for “no silent data loss, ever,” because it reproduces the Creighton class of silent-logout overwrite failure. Rationale: the backend remains opaque LWW, but KMP refuses to destroy local facts silently.

### ADR-0004 — Persistence shape

**Decision:** persist the contract-shaped blob as JSON plus sync metadata, not a fully normalized relational model. **Runner-up:** normalize profiles, days, categories, and checked indices into SQL tables. Rejected because the backend is explicitly opaque and forward-compat depends on round-tripping unknown fields [SYNC_CONTRACT.md](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/SYNC_CONTRACT.md). Rationale: JSON storage minimizes impedance mismatch, while repository methods still expose typed flows for Compose.

## Operational verification plan tied to SYNC_CONTRACT.md §9

Create three layers. **Unit/property layer:** date-key tests exactly match §6 vectors; category/preset tests assert all 13 IDs and 5 diets; serializer tests re-emit fixtures without unknown-field loss; merge property tests assert idempotence `merge(a,a)=a`, commutativity `merge(a,b)=merge(b,a)` for index sets, associativity for three-device edits, monotonicity `local ⊆ merged` and `server ⊆ merged`, and deletion contestability (if one side removes an index while the other keeps it, union keeps it and logs a “remote deletion not silently applied” conflict). **Contract layer:** use a disposable account against `https://dailydozen.stephens.page/api`; PUT a KMP blob, load web, verify days/checks/diet/customServings; edit web, GET in KMP; re-PUT unknown-field fixture and confirm no field drop; prove LWW by updatedAt ordering; and prove 401 refresh does not logout-loop [SYNC_CONTRACT.md](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/SYNC_CONTRACT.md). **UI/platform layer:** Android emulator login/edit/offline/logout/export tests via UIAutomator XML dumps; Wasm browser tests for localStorage token and offline banners; Mac-only iOS smoke for Keychain persistence, app reinstall behavior, and `initKoin()` launcher initialization.

CI gates should be explicit: `:composeApp:allTests` where supported, Android unit tests, Wasm browser distribution, JSON fixture diff, dependency lock diff, and a “no accidental token logging” grep over source and test logs. Release gating should require a manual checklist for live backend tests because rate limits and real accounts make them unsuitable for every PR.

## Threat model and failure modes

| Threat/failure mode | Impact | Mitigation | Verification |
|---|---:|---|---|
| Silent pull overwrites weeks of local data | Critical | Existing-device merge/adopt decision; never replace local without `SyncDecision` | Login-with-local-data test |
| 401 refresh loop clears token and triggers fresh pull | Critical | One refresh attempt; auth-only signed-out state; preserve blob | Expired-token integration test |
| Keychain/Keystore cleared by OS, restore, or app reinstall | High | Treat as signed out with pending local data; login merges, not adopts | Platform storage tests |
| Replay/stolen JWT | High | HTTPS only, Keychain/Keystore, no logs, clear on logout, 30-day awareness; no biometric “security theater” unless it gates local app access separately | Secret/log scan |
| Biometric fallback confusion | Medium | Do not require biometrics for token use; optional app lock only; fallback passcode documented | UX review |
| 429 auth hammering | Medium | Backoff, surface `{error}`, disable submit temporarily | Mock API test |
| Payload >2 MB | Medium | Preflight byte-size check, export path, no repeated PUT | Large fixture test |
| Unknown future web fields dropped | High | JsonObject retention, fixture diff | Round-trip test |
| Time-zone date drift near midnight | Medium | Match JS local `toDateString()`, no UTC normalization | Date vectors |
| Wasm storage exposure | Medium | Label localStorage risk; no stronger claim | Security review |

## Scope discipline and hard limits

**What this work legitimately proves:** KMP `expect`/`actual` storage, Ktor auth, CI gates, and offline-first reliability are legible infrastructure engineering. It also yields one teachable artifact: **"ADR: Offline-first sync without silent data loss against an LWW opaque-blob backend."** Outline: context, constraints, the LWW backend, the prior silent-logout failure, merge algebra, token failure loops, UX contestability, test vectors, and "when I would choose server-side CRDT instead."

**What it does not justify:** hero scope. This is a supporting infrastructure proof, not a platform. Spending more than a bounded sprint here would turn a clean pattern into a product rabbit-hole. Grade: **B as a supporting infrastructure proof if capped; D if it becomes a product rabbit-hole**.

## Explicit out of scope

No new backend, database schema, cloud provider, certificate system, CRDT service, social auth in v1, biometric app lock in v1, analytics platform, push notifications, subscription/payments, new diet science features, shared-family collaboration, server-side account deletion unless the existing backend already supports it, and no public claim that this is a safe-AI automation artifact. The project is complete when sign-in, safe sync, export/delete transparency, and §9 verification are done—not when Daily Dozen becomes a flagship product.
