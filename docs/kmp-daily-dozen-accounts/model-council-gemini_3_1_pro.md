
# Implementation Plan: Daily Dozen KMP Sync (Gemini 3.1 Pro Analysis)

This document provides a rigorous, phased implementation plan for adding account sign-in and cross-device sync to the Daily Dozen KMP app. As one of the models on the Model Council, my specific analytical angle focuses on contextualizing architectural decisions within the broader KMP and mobile ecosystem, prioritizing concrete library choices, and establishing a bulletproof, mathematically sound data-merge strategy.

This plan is deliberately narrow: it aggressively avoids scope creep (e.g. rejecting social auth) and maximizes transferable engineering value (KMP secure storage, offline-first sync patterns, and data dignity).

---

## 1. Architectural Decision Records (ADRs)

*Context from the wider ecosystem:* Building offline-first KMP applications often forces developers to choose between heavy frameworks (like Realm or MongoDB Mobile) and bespoke sync logic. Because Daily Dozen is lightweight and must interface with an existing, highly specific [Node.js backend blob store](https://github.com/JacobStephens2/daily-dozen-kmp/blob/master/SYNC_CONTRACT.md), we must reject heavy abstractions in favor of surgical, targeted solutions. This approach maximizes transferable learning while respecting the constraints of the legacy system.

### ADR 1: Authentication Method
*   **Recommendation:** Reuse the existing Node.js JWT email/password backend (`/api/register`, `/api/login`).
*   **Rationale:** Daily Dozen is a demoted project. Implementing Magic Links (via Resend) or Social Login (Sign In With Apple / Google) would require backend migrations, App Store capability entitlement management, and UI overhauls. Crucially, [App Store Review Guideline 4.8](https://developer.apple.com/app-store/review/guidelines/#sign-in-with-apple) mandates that if you provide a social login (like Google), you **must** also implement Sign In With Apple. If you rely solely on your own proprietary email/password system, you are exempt from this requirement. This keeps the scope strictly bounded.
*   **Runner-up:** Magic Links via Resend. Excellent UX, but requires backend changes and SMTP wiring that detracts from the goal of learning KMP sync.

### ADR 2: Secure Token Storage Expect/Actual
*   **Recommendation:** Use `russhwolf/multiplatform-settings` with encrypted wrappers on native and `localStorage` on Wasm.
*   **Rationale:** KMP secure storage requires platform-specific implementations.
    *   **Android:** Provide an `AndroidSettings` instance backed by `EncryptedSharedPreferences` (AES256-GCM).
    *   **iOS:** Provide a `KeychainSettings` instance (backed by `SecItemAdd` / Security framework).
    *   **Wasm:** Provide a standard `StorageSettings` backed by the browser's `localStorage`. This intentionally lacks encryption, but explicitly aligns with the PWA's existing behavior (`localStorage.getItem('token')`), as per the Sync Contract.
*   **Runner-up:** `KVault`. A solid alternative, but `multiplatform-settings` is more ubiquitous in the KMP ecosystem and its `NoArg` module easily accommodates the unencrypted Wasm fallback.
*   **Token Lifecycle:** Use Ktor's `Bearer` auth plugin with the `refreshTokens` callback. If the API returns 401, Ktor will automatically pause requests, hit `/api/refresh-token`, store the new token via `multiplatform-settings`, and replay the failed request. On explicit logout, wipe the token and the local JSON blob, but prompt the user about unsynced local data first ("Data Dignity").

### ADR 3: Sync Model (CRDT Union Merge vs. Plain LWW)
*   **Recommendation:** Implement a hybrid **Client-Side Union Merge** applied before pushing a Last-Writer-Wins (LWW) blob.
*   **Rationale:** The user mandate is **"no silent data loss, ever"**. The server's `/api/data` endpoint is strictly LWW (`PUT` overwrites). If Device A and Device B both log data offline and then sync, a plain LWW push will clobber one device's work. 
    However, analyzing the payload schema reveals that `data[dateKey][categoryId]` is a *Set of checked indices*. This structure forms a State-based CRDT (specifically, a Grow-Only Set or G-Set for a given day, as users generally add checks. If unchecking is common, it operates effectively as an Observed-Remove Set or OR-Set, provided we track removal tombstones locally, though a simple Union often suffices for daily checklists where the ultimate goal is completion).
    By implementing a safe local-merge layer, we bridge the gap between a "dumb" server and a "smart" client. 
    *Implementation Details:* When `sync()` runs, it performs a `GET /api/data`. Before writing the `serverData` to the local SQLDelight database, or pushing back to the server, the client computes the mathematical union of the indices for every `dateKey` and `categoryId`:
    `mergedIndices = localIndices.toSet() union serverIndices.toSet()`
    The client then issues a `PUT /api/data` with the merged blob. The server remains blissfully unaware, continuing its LWW behavior, but the client mathematically guarantees no offline logging is ever dropped. This demonstrates a highly transferable skill: adapting CRDT principles to legacy REST endpoints without requiring backend migrations.
*   **Runner-up:** Pure CRDTs (e.g., Automerge/Yjs). Absolute overkill for this domain. The server is strictly an opaque JSON blob store. We cannot run a CRDT backend process, and shipping a massive CRDT history tree inside the 2MB JSON blob limit would eventually crash the app and break compatibility with the live web PWA, which expects a simple JSON structure.
*   **Recommendation:** Implement a hybrid **Client-Side Union Merge** applied before pushing a Last-Writer-Wins (LWW) blob.
*   **Rationale:** The user mandate is **"no silent data loss, ever"**. The server's `/api/data` endpoint is strictly LWW (`PUT` overwrites). If Device A and Device B both log data offline and then sync, a plain LWW push will clobber one device's work. 
    However, analyzing the payload schema reveals that `data[dateKey][categoryId]` is a *Set of checked indices*. This structure forms a State-based CRDT (specifically, a Grow-Only Set or G-Set for a given day).
    *Implementation:* When `sync()` runs, pull the server blob. Before writing to the local DB or pushing back to the server, compute the union of indices for every `dateKey` and `categoryId`:
    `mergedIndices = localIndices.toSet() union serverIndices.toSet()`
    Then `PUT` the merged blob. The server remains blissfully unaware (plain LWW), but the client guarantees no offline logging is ever dropped.
*   **Runner-up:** Pure CRDTs (e.g., Automerge/Yjs). Absolute overkill. The server is an opaque JSON blob store. We cannot run a CRDT backend, and shipping a CRDT history tree inside the JSON blob would break compatibility with the live web PWA.

### ADR 4: Local Persistence Migration
*   **Recommendation:** Migrate from normalized SQL (`servingLog(day, categoryId, count)`) to a single-row document store via `kotlinx.serialization` stored in SQLDelight (or `multiplatform-settings`).
*   **Rationale:** The Sync Contract mandates forward compatibility: unknown fields in the JSON blob must not be dropped. A normalized SQL schema would require a column for every possible JSON property, dropping anything it doesn't recognize. By storing the *raw parsed Kotlin object* (using `@Serializable` with `ignoreUnknownKeys = false` or a raw `JsonElement` property), we achieve a lossless round-trip.
    *Implementation:* Keep the Koin `TrackerRepository` architecture. Update `Bountywell.sq` to `CREATE TABLE accountState (id INTEGER PRIMARY KEY, profileBlob TEXT NOT NULL)`. The `Flow` now emits the deserialized object.

---

## 2. Phased Implementation Plan

### Milestone 1: Domain Parity & Date Formatting (Prep)
*   **Fix Catalog:** Update `CategoryCatalog.kt` to use the 13 exact hyphenated IDs from §4 (e.g., `beans`, `protein`, `other-fruits`).
*   **Fix Presets:** Implement the 5 `dietType` presets (§5). The catalog UI must dynamically render based on the active profile's `customServings` or fallback to `dietType`.
*   **Fix Date Keys:** Replace `Clock.System.todayIn().toString()` with a `toJsDateString()` extension function. **Crucial:** Use the device's local timezone. Do not normalize to UTC.
    *   *Exit Criteria:* Unit tests pass the 8 test vectors in §6. The UI correctly renders the 13 categories.

### Milestone 2: Lossless Persistence & Authentication Storage
*   **Data Model Migration:** Delete the `servingLog` SQL schema. Replace it with `profileBlob TEXT`.
*   **Indices vs Counts:** Update `ChecklistViewModel` and `ChecklistScreen` to manage *Sets of indices* rather than raw counts. Tapping a checkbox adds/removes its index.
*   **Token Storage:** Add `multiplatform-settings` with expect/actual configurations (Keychain, EncryptedSharedPreferences, localStorage).
    *   *Exit Criteria:* App can persist checked indices across reboots. KMP modules successfully compile with platform-specific encrypted storage.

### Milestone 3: Ktor Networking & Sync Engine
*   **API Client Setup:** Integrate Ktor. Use `ktor-client-core` and `ktor-client-content-negotiation` with `kotlinx-serialization`. Crucially, select the correct engines: `darwin` for iOS, `okhttp` for Android, and `js` for Wasm. (Note: Wasm requires specific handling for CORS and fetch API nuances).
*   **Auth Interceptor & Token Refresh:** Implement the Ktor `Bearer` auth plugin. Configure the `refreshTokens` block to automatically hit `/api/refresh-token` when a 401 is encountered, seamlessly updating the token in `multiplatform-settings` and replaying the failed request.
*   **The Merge Engine:** Implement the mathematical G-Set Union Merge for the JSON payload.
    ```kotlin
    // Conceptual signature for the merge logic
    fun mergePayloads(local: ProfilePayload, server: ProfilePayload): ProfilePayload {
        // Iterate through all dateKeys and categoryIds, performing set unions on the indices
        // Keep the latest 'updatedAt' timestamp
        // ...
    }
    ```
*   **Sync Timing & Debounce:** Implement the 3000ms debounced push (using Kotlin Coroutines `delay` and `Job` cancellation). Trigger an immediate pull upon successful login to hydrate a fresh device.
    *   *Exit Criteria:* Logging into a test account pulls data from the server. Checking a box queues a push. If the app is closed before 3 seconds, the data is persisted locally and will push on next launch.
*   **API Client:** Add `ktor-client-core`, `ktor-client-content-negotiation`, and platform engines (`darwin` for iOS, `okhttp` for Android, `js` or `wasm` for web).
*   **Auth Interceptor:** Implement the `Bearer` plugin.
*   **The Merge Engine:** Implement the G-Set Union Merge.
    ```kotlin
    // Conceptual signature
    fun mergePayloads(local: ProfilePayload, server: ProfilePayload): ProfilePayload
    ```
*   **Sync Logic:** Implement the 3000ms debounce push and the on-login immediate pull.
    *   *Exit Criteria:* Logging into a test account pulls data from the server. Checking a box pushes the payload after 3 seconds.

### Milestone 4: Account Lifecycle UX
*   **Screens:** Add Login, Register, Forgot Password.
*   **Data Dignity Prompt:** If a user clicks Logout, evaluate if `localSyncTs < localModifiedTs`. If true, show a dialog: "You have unsynced data. Logging out will delete it from this device. Sync now?"
    *   *Exit Criteria:* Full flow: Open app -> Log some data -> Create account -> Data is merged and synced -> Logout -> Data is wiped locally.

---

## 3. Engineering Value and Scope Discipline

This implementation is calibrated around high-leverage architectural patterns rather than feature bloat.

*   **High-Value Transferable Skills:**
    1.  **Offline-first multiplatform data synchronization:** building sync engines that handle network partitions without losing user data.
    2.  **KMP secure token management:** the `expect/actual` pattern bridging iOS Keychain, Android `EncryptedSharedPreferences`, and Wasm `localStorage`.
    3.  **Ktor Multiplatform Networking:** the quirks of the Darwin, OkHttp, and JS/Wasm engines, particularly around auth refresh.
*   **Data Dignity & Trust:** prompting before destructive logouts ("You have unsynced data") treats the user's data as theirs, not the app's.
*   **Honest "Does NOT Help":** a polished login UI, Compose animation tweaks, and OAuth providers do nothing for the architecture. UI work is bounded to raw Material 3 scaffolds so the focus stays on the sync contract.
*   **Right-Sized Scope:** no offline push-queues, no WebSockets, no background sync workers (`WorkManager`, `BGTaskScheduler`). The 3000ms foreground debounce is sufficient and matches the existing web PWA contract.
*   **Teachable Artifact:** this plan yields one engineering write-up/ADR: *Offline-first sync without data loss against a last-writer-wins opaque-blob backend.* It shows how to apply CRDT-inspired logic (G-Set union) onto a legacy REST endpoint without modifying the server.

---

## 4. Risks & Data-Loss Failure Modes

| Scenario | Risk / Failure Mode | Mitigation Strategy |
| :--- | :--- | :--- |
| **Concurrent Offline Logging** | Device A logs at 9am. Device B logs at 10am. Device A comes online, pushes. Device B comes online, pushes LWW blob, erasing A. | **The Union Merge.** Device B's sync pulls A's data, locally merges it with B's offline data via set union, and pushes the combined blob. |
| **Unknown Field Dropping** | Web PWA adds a `"streak"` field to the JSON. KMP app parses it, ignores it, and pushes a new blob missing `"streak"`. | Use `kotlinx.serialization` with `Json { ignoreUnknownKeys = false }` or capture raw properties in a `JsonObject` property to pass them through untouched. |
| **JS Date Timezone Shift** | Device A is in NY (EST), Device B is in LA (PST). At 11pm PST, it is 2am EST. They generate different `dateKey`s for the same moment. | **Acceptable by Design.** The Sync Contract §6 explicitly states: *"Match the web... two devices in different zones near midnight can use different keys."* Do not attempt to fix this with UTC normalization. |
| **Payload Cap Exceeded** | `PUT` body exceeds 2MB over years of logging. | **Out of Scope** for this specific milestone, but surface the 413/400 error gracefully to the user rather than crashing. |

---

## 5. Verification Plan (Tied to SYNC_CONTRACT §9)

1.  **§6 Test Vectors:** Write a Kotlin `@Test` running `toJsDateString()` against the 8 dates. Verify zero-padding (e.g., `04` not `4`).
2.  **Round-trip Compatibility:** 
    *   Log data on KMP Android.
    *   Open `https://dailydozen.stephens.page` in Chrome.
    *   Verify the exact same checkboxes are checked.
    *   Change a diet preset on the Web.
    *   Open KMP Android, verify the UI updates to reflect the new category targets.
3.  **Forward-Compat Test:** Manually inject `"future_feature": true` into the server SQLite DB. Open KMP app, let it pull, check a box, let it push. Verify `"future_feature": true` remains in the server DB.
4.  **No-Data-Loss Test:** Turn on Airplane Mode on Android. Turn on Airplane Mode on Web. Check "Beans" on Android. Check "Berries" on Web. Turn Web online -> syncs. Turn Android online -> syncs. Verify *both* Beans and Berries are checked on both platforms.

---

## 6. Out of Scope

*   **Background Sync:** We will not use WorkManager or `BGTaskScheduler`. Sync only happens while the app is in the foreground.
*   **Sign In With Apple / Google:** Strict adherence to email/password JWT.
*   **SQLite Normalization:** We abandon a relational schema for the synced data. The `profileBlob` is a document store.
*   **Complex Conflict Resolution UI:** The union merge handles checkboxes. For scalar values (like `name` or `dietType`), standard LWW applies. We will not build a "diff review" UI for users.


## Conclusion

By prioritizing a mathematical union merge on the client side, we fulfill the strict 'no data loss' requirement without needing to touch the fragile, legacy Node.js backend. The strategic use of `multiplatform-settings` and Ktor's built-in auth plugins keeps the architecture clean and cross-platform. This plan provides a concrete, phased roadmap to production.