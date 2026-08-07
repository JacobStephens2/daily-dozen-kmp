# Daily Dozen KMP — Account Sign-In & Cross-Device Sync: Implementation Plan

**Model:** Claude Opus 4.8 (Model Council member) · **Role:** Senior staff platform/mobile engineer, planning agent
**Scope:** Add email/password account sign-in and cross-device sync to the existing Compose Multiplatform "Daily Dozen" app, synchronizing with the **same** account + opaque-blob backend as the live web PWA.
**Authoritative spec:** [`SYNC_CONTRACT.md`](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/SYNC_CONTRACT.md). Where I deviate (the safe-merge layer), I say so and justify web compatibility.
**Scope frame:** this is a small hobby tracker, not a platform. The plan is right-sized accordingly, and states plainly where the work does *not* generalize.

> Planning only. No implementation code beyond tiny illustrative signatures.

---

## 0. What I verified in the codebase (load-bearing facts)

I cloned the repo and read the actual files; the design below rests on these confirmations, not paraphrase:

- **The backend is a single opaque JSON blob with no schema validation and no field-level merge** — the server just `JSON.parse`/`JSON.stringify` ([`SYNC_CONTRACT.md` §3, §7](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/SYNC_CONTRACT.md)). Sync only works if the client writes byte-compatible JSON.
- **`data[dateKey][categoryId]` is an array of checked serving *indices* (a set), not a count** — the web pushes in tap order via `.push(idx)` and the contract instructs clients to "de-dupe and treat as a set" ([`SYNC_CONTRACT.md` §3](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/SYNC_CONTRACT.md)). This single fact is what makes a lossless local merge possible.
- **The current local schema is count-based and incompatible with the blob model:** `servingLog(day, categoryId, count)` with `INSERT OR REPLACE` ([`Bountywell.sq`](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/composeApp/src/commonMain/sqldelight/page/stephens/bountywell/db/Bountywell.sq)). A count cannot represent *which* checkboxes are ticked, so it cannot round-trip through the contract's index-set blob.
- **The current catalog is wrong for sync:** [`CategoryCatalog.kt`](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/composeApp/src/commonMain/kotlin/page/stephens/bountywell/domain/CategoryCatalog.kt) hard-codes 12 categories with **underscored** IDs (`other_fruits`, `nuts_seeds`) and **omits `protein`**. The contract requires 13 **hyphenated** IDs including `protein` ([`SYNC_CONTRACT.md` §4](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/SYNC_CONTRACT.md)). Every underscore is a silent sync failure.
- **The architecture to preserve is real and clean:** Koin `appModule` + `expect val platformModule`, `initKoin()` as the single launcher entry point ([`AppModule.kt`](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/composeApp/src/commonMain/kotlin/page/stephens/bountywell/di/AppModule.kt)); `expect class DatabaseDriverFactory` with three actuals ([`DatabaseDriverFactory.kt`](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/composeApp/src/commonMain/kotlin/page/stephens/bountywell/data/DatabaseDriverFactory.kt)); `TrackerRepository` opening the DB lazily behind a `Mutex` and exposing reactive `Flow`s ([`TrackerRepository.kt`](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/composeApp/src/commonMain/kotlin/page/stephens/bountywell/data/TrackerRepository.kt)).
- **iOS launcher still lacks `initKoin()`** and is Mac-only to build ([`SESSION_STATE.md`](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/SESSION_STATE.md), [`SYNC_CONTRACT.md` §8](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/SYNC_CONTRACT.md)).
- **Wasm SQLDelight is in-memory only** — the sql.js worker DB does not survive a full page reload ([`SESSION_STATE.md`](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/SESSION_STATE.md)). This interacts dangerously with sync (see §Risks).
- **CI exists** (`ci.yml`, `release.yml`) and builds Android + Wasm on every push, with signed releases built entirely in CI ([`README.md`](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/README.md)).

The non-negotiable lesson is real: the sibling Creighton/Chart35 tracker shipped sync that overwrote local data, and a real user lost weeks of data after a silent logout ([`creighton_tracker.md`](https://github.com/JacobStephens2/daily-dozen-kmp)). **"No silent data loss, ever" is a hard requirement** that must be reconciled against the backend's destructive last-writer-wins (LWW) model.

---

## 1. The core architectural decision: safe-merge over LWW — and why it is provably safe

The contract's §7 sync is whole-blob LWW with pull-on-login that **overwrites local state**. Adopting it verbatim re-creates the exact Chart35 failure mode: a stale/empty local store (or a fresh re-install, or a Wasm reload) pulls the server blob and clobbers unsynced local edits; symmetrically, a device that logs in with newer local edits but an older `updatedAt` can push and clobber the server. I recommend a **safe-merge layer over the LWW backend**, and the rest of this section proves the merge is lossless *and* produces a byte-compatible blob.

### 1.1 The data model is a join-semilattice

For a fixed `(profileId, dateKey, categoryId)`, the stored value is a set of checked serving indices `S ⊆ ℕ₀`. Define the merge of two observations of the same cell as set union:

```
mergeCell(a: Set<Int>, b: Set<Int>): Set<Int> = a ∪ b
```

Set union has the three algebraic properties that make conflict resolution **order-independent and replay-safe**:

- **Commutative:** `a ∪ b = b ∪ a`. The result does not depend on which device synced first.
- **Associative:** `(a ∪ b) ∪ c = a ∪ (b ∪ c)`. Three-way and N-way merges need no coordination or sequencing.
- **Idempotent:** `a ∪ a = a`. Re-applying the same blob (e.g., a duplicate pull, a retried PUT) changes nothing.

A set under union with these properties is a **bounded join-semilattice**; merging two replicas is the *join*. This is exactly the algebraic structure of a state-based CRDT (a grow-only set, G-Set, per cell). Jacob has shipped a G-Counter CRDT before on a separate listening-time feature, so this is a known idiom, not new research — and it is the *simpler* sibling (G-Set vs G-Counter, no per-replica vector needed because the elements themselves are the identity).

The whole blob merges cell-by-cell: `mergeBlob = ⋃ over all (profile, date, category) of mergeCell`. Because each cell merge is a semilattice join and the blob is a finite product of independent cells, the product is itself a semilattice — so **the whole-blob merge inherits commutativity, associativity, and idempotence**. There is no cross-cell interaction to break the property.

### 1.2 Why this is still web-compatible (the deviation, stated honestly)

This is a **deliberate deviation from §7's "don't invent a smarter merge"**, and here is why it is safe rather than divergent:

1. **The merge output is structurally identical to a web-authored blob.** Union of integer index sets, serialized as a JSON array of integers, is exactly the shape §3 specifies. The server stores it verbatim; the web app reads `data[dateKey][categoryId]` as an array and renders checkboxes. The web cannot tell a merged array from a hand-tapped one — both are just "which indices are checked."
2. **Sort and de-dupe on serialize** so output is canonical (`[0,1,2]`, never `[2,0,2]`). The web tolerates any order (it iterates), but canonical output keeps round-trip diffs clean and unit tests deterministic.
3. **Forward-compat is preserved.** Unknown profile fields and unknown top-level keys are carried through untouched (parse to a model that retains an `unknownFields` bag; re-emit on serialize). The merge only touches `data[date][category]` index arrays; everything else is passed through, so a future web change does not get dropped.
4. **The merge is a strict refinement of LWW, not a replacement of the protocol.** Transport is still GET/PUT of the whole blob with `updatedAt` ordering. The only change is: *before* deciding pull-vs-push, when both sides have diverged we compute `merge(local, server)` and PUT the union, then store the new `updatedAt`. Profile-level scalar fields that are genuinely single-valued (`name`, `color`, `dietType`, `customServings`) remain LWW by `updatedAt` — only the additive log is union-merged. This bounds the deviation to exactly the field where loss is unacceptable.

**Net:** the safe-merge layer is conflict-free, lossless for the additive log, and emits blobs the web reads correctly. The cost is one merge function plus a "both diverged" branch in the sync state machine — small, testable, and the single highest-leverage safety decision in the project. (Full ADR in §ADR-3.)

The one honest caveat: union merge means a check *un-checked* on device A while device B still has it checked will **reappear** after merge (un-checks don't propagate; G-Sets are grow-only). For a daily food tracker this is the correct trade — accidental data *loss* is catastrophic, an accidental extra check is trivially re-tapped. If un-check propagation is ever required, it is a 2P-Set or LWW-per-element upgrade, explicitly deferred (§Out of scope).

---

## 2. ADR-style decision records (items 1–4)

### ADR-1 — Authentication method: reuse the existing JWT email/password backend

**Status:** Accepted. **Context:** The KMP app must share accounts with the web PWA, which already has `register`/`login`/`refresh-token`/`forgot-password`/`reset-password` with a 30-day HS256 bearer JWT ([`SYNC_CONTRACT.md` §2](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/SYNC_CONTRACT.md)). Auth infra already exists and is shared server-side; the marginal cost of reuse is a thin Ktor client.

**Decision:** Reuse JWT email/password exactly as specified. No new auth infrastructure, no new identity provider.

**Alternatives considered:**
- *Magic-link (Resend SMTP is wired):* better UX, no password to lose, and the email path already exists for password reset — but it requires a new server endpoint and deep-link handling on three platforms (custom URL schemes + universal/app links + Wasm route), and it diverges the KMP and web auth flows. **Runner-up**, deferred; not worth the cross-platform deep-link cost for a demoted app.
- *Sign in with Apple / Google (social/OAuth):* would require new backend identity federation, OAuth client registration per platform, and — critically — **App Store Guideline 4.8** forces offering Sign in with Apple if you offer any third-party social login on iOS, plus its privacy-relay/anti-tracking requirements. This is a large surface for zero benefit here. **Rejected.**

**Consequences:** Zero new server work; the KMP client is a strict subset of the web's auth behavior, which de-risks parity. Password ≥ 8 chars, `400` invalid email, `409` duplicate, `429` rate-limited must all be surfaced from `{error}` ([§2](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/SYNC_CONTRACT.md)). Cost: passwords are a worse UX than magic links and a phishing/breach surface — accepted for a hobby tracker reusing existing infra.

### ADR-2 — Secure token storage via `expect`/`actual`, with refresh that never logs you out

**Status:** Accepted. **Context:** A 30-day JWT must be stored securely per platform and refreshed within ~7 days of expiry (matching the web) ([§2](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/SYNC_CONTRACT.md)). The Chart35 disaster was triggered by a **silent logout**, so token handling is on the critical path for the data-loss requirement.

**Decision:** Introduce a `TokenStore` `expect`/`actual` abstraction wired through Koin's existing `platformModule` (the same seam that already provides `DatabaseDriverFactory`):

```kotlin
expect class TokenStore {
    suspend fun read(): String?
    suspend fun write(token: String)
    suspend fun clear()   // ONLY called on explicit user logout — never on a 401
}
```

- **iOS:** Keychain (`kSecClassGenericPassword`, `kSecAttrAccessibleAfterFirstUnlock`).
- **Android:** EncryptedSharedPreferences (Jetpack Security) backed by the Android Keystore; fall back to Keystore-wrapped key if EncryptedSharedPreferences is unavailable.
- **Wasm:** `localStorage` under the **same key the web app uses** so the web and Wasm builds share a session on the same origin (matches §2's "Wasm → localStorage (matches web)").

**Alternatives:** multiplatform-settings with a single encrypted backend (less control over iOS Keychain accessibility flags; **runner-up**); in-memory only (loses session every launch — rejected).

**Consequences (the load-bearing rule):** **A `401` must trigger a single silent refresh attempt, never a logout and never a local wipe.** The auth state machine is: `401 → try /refresh-token → on success, replay the request → on refresh failure, enter a "needs re-auth" state that keeps all local data and shows a non-destructive banner.** `clear()` is invoked *only* from the explicit logout UI, *after* a confirmed push of pending local changes. This single consequence is the difference between this plan and the Chart35 outage.

### ADR-3 — Sync model: safe local-merge (union of checked indices) over LWW transport

**Status:** Accepted (deliberate, documented deviation from §7). **Context:** §7 is destructive whole-blob LWW. The hard requirement is no silent data loss. §3 guarantees the log cell is a *set* of indices, which is mergeable. **Decision:** Implement the join-semilattice merge formalized in §1: per-`(profile, date, category)` set union for the additive log; `updatedAt`-LWW for genuinely single-valued profile scalars; carry unknown fields through untouched.

**Sync state machine:**
- **On login (fresh device, no local data):** pull and adopt server blob wholesale (this matches §7 and is correct — there is nothing to lose).
- **On login (existing device with local data):** GET server; if `serverUpdatedAt > localSyncTs` *and* local has unsynced edits → **merge**, PUT the union, store returned `updatedAt`. If only one side changed, behave as plain pull or push.
- **On local edit:** mark dirty, debounce **3000 ms** (matches §7's coalescing window), then sync.
- **Conflict (both diverged):** always merge, never clobber.

**Alternatives:** *Plain LWW per §7* (simplest, web-identical, but **re-creates the Chart35 data-loss bug** — rejected as the default, retained only for the fresh-device-adopt branch). *Full op-based CRDT log* (overkill; the state-based G-Set is sufficient and matches Jacob's prior CRDT work — **runner-up**, rejected as over-engineering for a demoted app).

**Consequences:** One additional merge branch and one merge function (~40 lines) plus tests. Web compatibility preserved (§1.2). Un-checks don't propagate (accepted trade, §1.2). This is the **teachable artifact** (§8).

### ADR-4 — Local persistence migration: count-schema → contract blob model, losslessly

**Status:** Accepted. **Context:** [`Bountywell.sq`](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/composeApp/src/commonMain/sqldelight/page/stephens/bountywell/db/Bountywell.sq) stores `count`, which cannot represent which indices are checked and cannot round-trip the §3 blob. §8 explicitly says to replace the count schema while keeping the architecture.

**Decision:** Model the §3 payload with `kotlinx.serialization` (a `SyncBlob` with `profiles: Map<String, Profile>`, each `Profile` carrying `name/color/dietType/customServings/data` plus an `unknownFields` bag) and **persist that JSON**, e.g. one SQLDelight text row per profile (or multiplatform-settings). This guarantees a lossless round-trip with the opaque server blob and preserves unknown fields by construction — exactly §8's "simplest sync-safe persistence" recommendation.

**Existing-data migration (must not lose the user's local count history):** add a one-time SQLDelight migration that reads existing `servingLog(day, categoryId, count)` rows and **synthesizes index arrays**: a `count = N` becomes indices `[0, 1, …, N-1]` for that `(day, category)`, under the default `"user"` profile with `dietType = "standard"`. This is information-preserving for the only thing a count can mean (N servings logged ⇒ N boxes checked). Old underscored category IDs are mapped to hyphenated IDs during migration. The migration is gated and idempotent; it writes the new store and leaves the old table until a verified first sync, so a failed migration never destroys the original rows.

**Alternatives:** normalized relational schema mirroring the blob (more code, lossy on unknown fields — rejected); wipe-and-resync from server on upgrade (**re-creates data loss for offline users** — rejected). **Runner-up:** multiplatform-settings instead of a SQLDelight text row (simpler, but loses the existing migration path and reactive query story — keep SQLDelight).

**Consequences:** `TrackerRepository` changes from `setCount`-oriented to blob-oriented (`toggleIndex(date, category, index)`, `loadBlob()`, `saveBlob()`), still behind the `Mutex`, still exposing reactive `Flow`s. The Koin wiring and `expect`/`actual` driver are untouched.

---

## 3. Catalog & date correctness (item 5)

Replace [`CategoryCatalog.kt`](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/composeApp/src/commonMain/kotlin/page/stephens/bountywell/domain/CategoryCatalog.kt) with the **13 hyphenated IDs including `protein`** and the **5 diet presets** verbatim from §4/§5. Active categories and per-category targets derive from the active profile's `dietType` (preset map) overridden by `customServings` when non-null; `getActiveCategories()` = entries with servings > 0. Date keys use the `toJsDateString()` formatter from §6 in **device-local timezone** (not UTC), and must pass all eight §6 test vectors including zero-padding and the 2024-02-29 leap day. The `protein`-omission and underscore bugs are the most likely silent sync breakers, so the catalog swap is an early, isolated, well-tested commit.

---

## 4. Account lifecycle UX — data dignity as a first-class flow (item 6)

The data-dignity angle (export, deletion, transparency, never destroy local without consent) is the most transferable UX value here, so it is designed explicitly rather than bolted on:

- **Register / Login:** standard forms; on first successful login on a fresh device, pull-and-adopt; on an existing device with local data, **merge** (never overwrite). Surface `{error}` from `400/409/429` plainly; never retry-hammer `429`.
- **Logout — the explicit data-safety prompt:** logout shows a modal: *"Your logged days are saved on this device and will stay here. We'll sync any unsaved changes before signing out."* Logout (a) flushes any pending push (or, if offline, warns and offers to stay signed in), then (b) clears the token, and (c) **leaves the local store intact**. Logout never wipes local data. This directly inverts the Chart35 failure.
- **Forgot / Reset:** call the existing endpoints; reset emails link to `stephens.page` (the backend's `APP_URL`) — the app just initiates and tells the user to check email.
- **Account deletion + data export:** **Export** serializes the local `SyncBlob` to a `.json` file via a platform share sheet (Android `Intent`, iOS `UIActivityViewController`, Wasm download) — contestability and portability. **Deletion** is a clearly-labeled destructive action with double-confirm; note honestly in the UI whether server-side deletion is supported by the backend (the contract exposes no delete endpoint — if absent, deletion is local-only and the user is told so, rather than implying a false server wipe).
- **Transparency:** a small "Sync status" line (last synced time from `updatedAt`, pending-changes indicator, offline state) so the user always knows whether their data is safe — the antidote to silent failure.

---

## 5. Networking, iOS, testing, CI (items 7–10, condensed)

- **Networking (7):** add Ktor with per-target engines (OkHttp/Android, Darwin/iOS, Js/Wasm) behind a `SyncApi` interface in `commonMain`, base URL `https://dailydozen.stephens.page/api`, an auth plugin that injects `Authorization: Bearer` and runs the ADR-2 refresh-on-401. Enforce the **2 MB PUT cap** client-side (if a blob approaches it, surface a clear error rather than a server reject); treat **offline as a normal state** — queue the dirty flag and sync when connectivity returns; never lose the queued edit.
- **iOS (8):** add `initKoin()` to `iOSApp.swift` (currently missing per §8/`SESSION_STATE.md`); add Keychain `TokenStore.ios` and Darwin Ktor engine. **Mac-only build/test** — call this out honestly as unverifiable on Jacob's Linux box; do not claim iOS is shipped.
- **Testing (9):** unit tests for the §6 date-key vectors (all eight, incl. leap day + zero-pad) and for merge laws (commutativity/associativity/idempotence via property tests, plus the un-check-reappears case as a documented expectation). Contract **§9 web round-trip**: write a blob from KMP, load in the web app on the same account; write from web, confirm KMP drops no fields and re-PUTs cleanly. Verify UI on the existing Android emulator via `uiautomator dump` (headless screencap returns black per the brief).
- **CI/security (10):** extend `ci.yml` to run the new merge + date-key + serialization tests on Android and Wasm. Security pass on token handling: assert `clear()` is reachable only from logout, never from a 401 path (a CI grep/test guard); confirm tokens never land in logs; keep builds reproducible (CI-only signing already established per [`README.md`](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/README.md)).

---

## 6. Phased implementation plan (milestones → exit criteria → atomic commits)

Smallest-safe-step ordering: correctness primitives first, then storage, then auth, then networking, then merge, then UX — so each milestone is independently verifiable and the risky sync step lands last on a proven base.

| Phase | Commits (atomic) | Exit criteria |
|---|---|---|
| **M0 — Catalog & date correctness** | (1) replace `CategoryCatalog` with 13 hyphenated IDs + `protein`; (2) add 5 diet presets + active-category derivation; (3) `LocalDate.toJsDateString()` + `todayKey()` | All §6 vectors pass; UI renders active categories from `dietType`; no underscores remain |
| **M1 — Blob model & persistence migration** | (4) `kotlinx.serialization` `SyncBlob`/`Profile` with `unknownFields`; (5) SQLDelight text-row store + repository rewrite to blob ops; (6) one-time `count → indices` migration (idempotent, non-destructive) | Round-trip a web-authored blob in-memory with zero dropped fields; old local data migrated, original table retained until first verified sync |
| **M2 — Secure token storage** | (7) `TokenStore` expect + 3 actuals (Keychain / EncryptedSharedPreferences / localStorage); (8) Koin wiring in `platformModule` | Token persists across launch on Android/Wasm; `clear()` only callable from logout (CI guard) |
| **M3 — Ktor auth client** | (9) `SyncApi` + per-target engines; (10) auth plugin w/ Bearer + **refresh-on-401, no logout** | register/login/refresh/data GET+PUT work against staging; expired-token → refresh → replay, never logout loop (§9.5) |
| **M4 — Safe-merge sync engine** | (11) `mergeBlob` semilattice join + property tests; (12) sync state machine (fresh-adopt / existing-merge / debounce 3s); (13) sync-status surfacing | §9.2–§9.4 web round-trip + LWW-ordering checks pass; both-diverged path merges, never clobbers |
| **M5 — Account lifecycle UX** | (14) auth screens; (15) logout w/ data-safety prompt + pre-flush; (16) export + (local) deletion | Logout never wipes local; export produces a valid re-importable blob; offline edit survives a logout/login cycle |
| **M6 — iOS + CI/security** | (17) `initKoin()` in `iOSApp.swift` + Darwin engine; (18) extend `ci.yml` tests + token security guard | Android+Wasm green in CI; iOS documented as Mac-only/unverified; security guard passes |

---

## 7. Risks & failure-modes — data-loss taxonomy first

| # | Failure mode | Trigger | Severity | Mitigation |
|---|---|---|---|---|
| **D1** | **Silent-logout wipe (the Chart35 outage)** | Token expiry/`401` treated as logout → local wipe or server-pull-overwrite | **Catastrophic** | ADR-2: `401`→silent refresh→replay; `clear()` only on explicit logout *after* push; logout never wipes local |
| **D2** | **Pull-overwrites-local on login** | Existing device with unsynced edits logs in; plain §7 pull clobbers | **Catastrophic** | ADR-3 merge on existing-device login; plain adopt only when local is empty (fresh device) |
| **D3** | **Push-clobbers-server** | Device with stale `updatedAt` but new local edits pushes whole blob | **High** | Merge before PUT when both diverged; never blind-PUT over a newer server blob |
| **D4** | **Wasm in-memory reload wipe** | sql.js worker DB doesn't survive page reload ([`SESSION_STATE.md`](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/SESSION_STATE.md)); reload → empty local → pull/adopt looks "correct" but un-synced edits are gone | **High** | Push (or persist to IndexedDB/localStorage) before unload; treat empty local as "needs pull," never as "authoritative empty to push" |
| **D5** | **Underscore / missing-`protein` silent divergence** | Wrong category IDs → web and KMP write disjoint keys; data "syncs" but never reconciles | **High** | M0 catalog swap + §9.2 web round-trip test before any sync ships |
| **D6** | **Date-key drift (UTC vs local, no zero-pad)** | Wrong `toDateString()` → edits land on the wrong day key vs web | **High** | §6 formatter + all 8 vector tests in CI |
| **D7** | **Unknown-field drop on round-trip** | KMP parses, re-PUTs, drops a web-added field | **Medium** | `unknownFields` passthrough (ADR-4); §9.3 no-dropped-fields test |
| **D8** | **Migration corruption** | `count→indices` migration crashes mid-way | **Medium** | Idempotent, gated; keep original `servingLog` table until first verified sync |
| **D9** | **2 MB cap rejection** | Large blob hits PUT limit, edit silently lost | **Low/Med** | Client-side size check; surface a clear error, keep local intact |
| **D10** | **Un-check doesn't propagate** | G-Set is grow-only; un-tap on one device reappears after merge | **Low (accepted)** | Documented trade (§1.2); never causes loss, only an extra re-tappable check |
| R1 | Rate-limit lockout | Retry-hammering `429` | Low | Surface `{error}`, exponential backoff, no auto-retry storms |
| R2 | iOS unverifiable | Linux-only dev box | Low | Mark iOS Mac-only; do not claim shipped |

---

## 8. Honest scope assessment

**What this is.** A CRUD hobby tracker syncing a food checklist against a backend that stores one opaque JSON blob. The interesting part is not the app — it is the constraint: making sync provably lossless against a last-writer-wins server you do not control.

**What it is not.** It is not distributed-systems work at scale, and the plan does not pretend otherwise. iOS remains marked unverified rather than claimed as shipping.

**The one teachable artifact.**
> **ADR / write-up: "Offline-first sync without data loss against a last-writer-wins opaque-blob backend."**
> 1. *Context:* an opaque-blob LWW server you don't control; a hard "never lose local data" requirement; a sibling app's real data-loss outage as motivation.
> 2. *The insight:* if the conflicting field is a *set*, per-cell union is a join-semilattice (commutative/associative/idempotent) — a state-based CRDT — so you can layer lossless merge on top of a dumb LWW transport without touching the server.
> 3. *The proof:* the three algebraic properties and why they make the merge order-independent and replay-safe.
> 4. *The boundary:* what merge does *not* fix (un-checks/grow-only; single-valued scalars stay LWW), stated honestly.
> 5. *The reusable KMP pattern:* secure-token `expect`/`actual` (Keychain / EncryptedSharedPreferences / localStorage) + Ktor refresh-on-401 that never logs you out.
> 6. *Data dignity:* export, local-deletion honesty, transparency, never-destroy-without-consent.

The instincts it exercises — human-in-control, never destroy without consent, contestable, auditable — transfer to any system where a machine reconciles state on a user's behalf.
---

## 9. Out of scope / do not build

- **No new auth infra:** no magic-link, no Sign in with Apple/Google, no OAuth — reuse JWT email/password (ADR-1).
- **No new backend endpoints, languages, clouds, or certs.**
- **No op-based CRDT / vector clocks / full causal log** — the state-based G-Set is sufficient (ADR-3 runner-up rejected).
- **No un-check propagation / 2P-Set** in v1 (documented trade, §1.2) — defer unless a real need appears.
- **No multi-profile editing UX beyond what the blob already encodes** (`user`/`other`) — sync both profiles, but don't build profile management.
- **No Wasm IndexedDB durability layer** unless web durability is explicitly wanted (note the reload caveat; gate sync to avoid D4).
- **No iOS shipping claims** — Mac-only build, documented as unverified.
- **No centerpiece treatment** — build it, write the one ADR, then stop.

---

## 10. Verification plan (tied to SYNC_CONTRACT.md §9)

1. **§9.1 — Date keys:** `toJsDateString()` passes all eight §6 vectors incl. zero-pad and 2024-02-29 leap day (CI unit test).
2. **§9.2 — Web round-trip out:** write a KMP-authored blob, load it in the **web app on the same account**; days/checks/`dietType`/`customServings` all render correctly.
3. **§9.3 — Web round-trip in (no dropped fields):** load a web-authored blob in KMP, re-PUT, confirm server `updatedAt` round-trips and no field is dropped (`unknownFields` passthrough test).
4. **§9.4 — LWW ordering:** edit on web → KMP pulls newer; edit on KMP → web pulls newer; **plus the merge extension:** edit on both → union appears on both, nothing lost.
5. **§9.5 — Token refresh:** expired token triggers refresh + replay, **never a logout loop** (and never a local wipe) — the explicit guard against the Chart35 failure.
6. **Merge laws (beyond §9):** property tests for commutativity, associativity, idempotence; documented un-check-reappears case.
7. **UI verification:** existing Android emulator via `uiautomator dump` (headless screencap returns black per the brief).

---

*Bottom line: adopt a provably-lossless join-semilattice merge over the existing LWW opaque-blob backend, store the §3 blob verbatim with `kotlinx.serialization` + a non-destructive migration, reuse the JWT backend with a refresh-on-401 token store that never logs you out, and right-size the whole effort to one teachable ADR — honestly acknowledging that this is a small hobby tracker, even as its data-dignity instincts rehearse a discipline that scales.*
