# Sync contract — Daily Dozen (KMP ⇄ web backend)

**Status:** authoritative. Derived from the live web app + backend source on the
server (`/var/www/dailydozen.stephens.page`: `api.js`, `db.js`, `js/auth.js`,
`js/storage.js`, `js/categories.js`) on 2026-05-31, not from paraphrase.

The KMP apps (iOS/Android/Wasm) sync with the **same** account + data store as the
web PWA. The backend stores each user's data as a **single opaque JSON blob** — it
does no schema validation (`api.js` just `JSON.parse`/`JSON.stringify`). Therefore
**the only thing that makes sync work is the client writing byte-compatible JSON.**
Match this document exactly. When in doubt, the web source wins — update this file
if the web app changes.

---

## 1. API base URL

Use **`https://dailydozen.stephens.page/api`**.

- Both `dailydozen.stephens.page` and `dailydozen.jacobstephens.net` proxy
  `/api/ → 127.0.0.1:3000` (same Node process, same SQLite DB), so accounts/data
  are shared either way — but `stephens.page` is canonical: it's the backend's own
  `APP_URL` (password-reset emails link there) and matches the rest of the infra.
- `jacobstephens.net` is a legacy alias. Older session notes calling it "the live
  URL" are stale (they reference `/var/www/daily_dozen/`, which no longer exists).
- The web client uses a **relative** `/api`; native clients must use the absolute
  base above.

---

## 2. Endpoints

All JSON. Auth is a **Bearer JWT** (HS256, 30-day expiry).

| Method | Path | Auth | Body | Success response |
|---|---|---|---|---|
| POST | `/api/register` | — | `{email, password}` | `201 {token, email}` |
| POST | `/api/login` | — | `{email, password}` | `200 {token, email}` |
| POST | `/api/refresh-token` | Bearer | — | `200 {token, email}` |
| GET  | `/api/data` | Bearer | — | `200 {data, updatedAt}` (data may be `null`) |
| PUT  | `/api/data` | Bearer | `{data}` | `200 {success:true, updatedAt}` |
| POST | `/api/forgot-password` | — | `{email}` | `200 {message}` |
| POST | `/api/reset-password` | — | `{token, password}` | `200 {message}` |

Notes / gotchas:
- **Header:** `Authorization: Bearer <token>`. Missing/expired → `401 {error}`.
- **Validation:** password **≥ 8 chars** (register & reset); invalid email → `400`;
  duplicate email on register → `409`. All errors are `{error: "<message>"}`.
- **Rate limits:** login/auth 10 / 15 min; register 5 / hour; reset endpoints
  limited too. Surface the `{error}` message; don't hammer on `429`.
- **Payload cap:** PUT body limited to **2 MB**.
- **Token storage:** iOS → Keychain; Android → EncryptedSharedPreferences/Keystore;
  Wasm → `localStorage` (matches web). Refresh via `/refresh-token` when within
  ~7 days of expiry (web does this).

---

## 3. The synced blob (THE critical part)

`PUT /api/data` body is `{ "data": <PAYLOAD> }`. The server stores `<PAYLOAD>`
verbatim and `GET /api/data` returns it as `{ "data": <PAYLOAD>, "updatedAt": "…" }`.

`<PAYLOAD>` shape (from `auth.js` `buildDataPayload` / `restoreFromPayload`):

```jsonc
{
  "profiles": {
    "user": {                     // profile id (string key). Defaults: "user", "other"
      "name": "You",              // display name (default "You" / "Other")
      "color": "#38672a",         // hex; defaults "#38672a" (user), "#7c5724" (other)
      "dietType": "standard",     // preset id, see §5; default "standard"
      "customServings": {         // map<categoryId,int>; MAY BE null (then derive from dietType preset)
        "beans": 3, "protein": 0, "berries": 1, "other-fruits": 3, "greens": 2,
        "cruciferous": 1, "other-vegetables": 2, "flaxseed": 1, "nuts-seeds": 1,
        "herbs-spices": 1, "whole-grains": 3, "beverages": 5, "exercise": 1
      },
      "data": {                   // the actual log: dateKey -> categoryId -> checked serving indices
        "Thu Jan 16 2025": { "beans": [0, 2], "greens": [0] },
        "Fri Jan 17 2025": { "beans": [0, 1, 2] }
      }
    },
    "other": { "name": "Other", "color": "#7c5724", "dietType": "standard", "customServings": null, "data": {} }
  }
}
```

Rules:
- Top-level key is **`profiles`** (object keyed by profile id string, NOT an array).
- `customServings` is **nullable**. If null, the active categories/targets come from
  the `dietType` preset (§5). If present, it overrides the preset per category.
- `data[dateKey][categoryId]` is an **array of checked serving indices** (0-based),
  NOT a count. A category with serving target `N` shows `N` checkboxes; the array
  holds which are checked. Completion for that category = `indices.length >= target`.
  De-dupe and treat as a set; the web pushes in tap order (`app.js`: `.push(idx)`).
- Empty day/category may be absent entirely (don't write empty arrays unless the web
  would). Absence == nothing logged.
- Round-trip must be lossless: read the blob, mutate, write it back without dropping
  unknown fields (forward-compat).

---

## 4. Categories (master list — 13, exact IDs)

From `categories.js` `getAllCategories()`. **IDs are hyphenated** — this is the most
common place to get sync wrong. (The KMP skeleton's `DozenCatalog` used underscores
and omitted `protein`; replace it with these.)

| id | name | icon |
|---|---|---|
| `beans` | Beans | 🫘 |
| `protein` | Protein | 🥩 |
| `berries` | Berries | 🫐 |
| `other-fruits` | Other Fruits | 🍎 |
| `greens` | Greens | 🥬 |
| `cruciferous` | Cruciferous Vegetables | 🥦 |
| `other-vegetables` | Other Vegetables | 🥕 |
| `flaxseed` | Flaxseed | 🌾 |
| `nuts-seeds` | Nuts and Seeds | 🥜 |
| `herbs-spices` | Herbs and Spices | 🌿 |
| `whole-grains` | Whole Grains | 🌾 |
| `beverages` | Beverages | 💧 |
| `exercise` | Exercise | 🏃 |

`name`/`icon`/`description`/`examples` are **UI-only** and not part of the synced
blob (sync uses ids + indices only) — but mirror them for parity. Note the web
descriptions contain non-breaking spaces (` `); cosmetic only.

---

## 5. Presets / diet types (5, exact IDs)

From `categories.js` `PRESETS`. Map of `categoryId → servings` (0 = category excluded
for that diet). `getActiveCategories()` = entries with servings > 0.

| preset id | label | beans | protein | berries | other-fruits | greens | cruciferous | other-vegetables | flaxseed | nuts-seeds | herbs-spices | whole-grains | beverages | exercise |
|---|---|--|--|--|--|--|--|--|--|--|--|--|--|--|
| `standard` | Standard Daily Dozen | 3 | 0 | 1 | 3 | 2 | 1 | 2 | 1 | 1 | 1 | 3 | 5 | 1 |
| `modified` | Modified | 0 | 2 | 1 | 3 | 2 | 1 | 2 | 0 | 1 | 1 | 3 | 5 | 1 |
| `one-bean` | One Bean | 1 | 1 | 1 | 3 | 2 | 1 | 2 | 0 | 1 | 1 | 3 | 5 | 1 |
| `one-bean-two-protein` | One Bean + Two Protein | 1 | 2 | 1 | 3 | 2 | 1 | 2 | 0 | 1 | 1 | 3 | 5 | 1 |
| `one-bean-two-protein-one-flax` | One Bean + Two Protein + Flax | 1 | 2 | 1 | 3 | 2 | 1 | 2 | 1 | 1 | 1 | 3 | 5 | 1 |

Unknown/missing `dietType` falls back to `standard`.

---

## 6. Date keys — `toDateString()` (the #1 sync landmine)

`dateKey` is JavaScript `Date.prototype.toDateString()` computed in the **device's
local timezone** (`app.js`/`history.js` use `new Date()` and `this.currentDate`).

Format: `"EEE MMM dd yyyy"` — 3-letter English weekday, 3-letter English month,
**zero-padded** day, 4-digit year, single spaces. Always English regardless of
device locale. **Local timezone** (do NOT normalize to UTC — match the web, even
though it means two devices in different zones near midnight can use different keys).

Weekdays: `Sun Mon Tue Wed Thu Fri Sat` · Months: `Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec`

### Test vectors (authoritative, generated from Node)

| Local date | dateKey |
|---|---|
| 2025-01-16 | `Thu Jan 16 2025` |
| 2025-01-01 | `Wed Jan 01 2025` |
| 2025-12-25 | `Thu Dec 25 2025` |
| 2024-02-29 | `Thu Feb 29 2024` |
| 2025-07-04 | `Fri Jul 04 2025` |
| 2025-09-09 | `Tue Sep 09 2025` |
| 2026-05-31 | `Sun May 31 2026` |
| 2025-03-02 | `Sun Mar 02 2025` |

### Reference Kotlin formatter (kotlinx-datetime) — unit-test against the table above

```kotlin
import kotlinx.datetime.*

private val JS_WEEKDAYS = arrayOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun") // isoDayNumber 1..7
private val JS_MONTHS = arrayOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")

/** Matches JS Date.prototype.toDateString(): "EEE MMM dd yyyy", English, zero-padded day. */
fun LocalDate.toJsDateString(): String {
    val wd = JS_WEEKDAYS[dayOfWeek.isoDayNumber - 1]
    val mo = JS_MONTHS[monthNumber - 1]
    val dd = dayOfMonth.toString().padStart(2, '0')
    return "$wd $mo $dd $year"
}

/** "Today" in the device's local zone — mirrors the web's `new Date()`. */
fun todayKey(clock: Clock = Clock.System, tz: TimeZone = TimeZone.currentSystemDefault()): String =
    clock.now().toLocalDateTime(tz).date.toJsDateString()
```

Parsing a key back to a date (for history/day-nav): map the abbreviations back, or
keep dates as `LocalDate` internally and only convert at the storage boundary.

---

## 7. Sync algorithm (whole-blob, last-writer-wins)

From `auth.js`. There is **no field-level merge** — the whole `profiles` blob is
replaced. Match this (don't invent a smarter merge that would diverge from the web).

- `updatedAt` is the server's SQLite `datetime('now')` → `"YYYY-MM-DD HH:MM:SS"` in
  **UTC**, lexicographically sortable. Persist the last-synced `updatedAt` locally
  (web key: `dailyDozenSyncTimestamp`).
- **`sync()`**: `GET /data`. If `serverData != null && serverUpdatedAt > localSyncTs`
  (string comparison) → **pull** (overwrite local, store new ts). Else → **push**.
- **`pushData()`**: build payload (§3), `PUT {data: payload}`, store returned `updatedAt`.
- **Debounced push**: after any local change, push **3000 ms** later (coalesce bursts).
- **On login**: pull immediately so a fresh device adopts the server state.
- Implication: concurrent edits on two devices clobber (acceptable, by design).

---

## 8. What this means for the current KMP code

- **Replaces** the count-based SQLDelight schema (`servingLog(day, categoryId, count)`)
  from the persistence commit. Keep the architecture (repository + Koin + expect/actual
  drivers) — only the schema + serialization change.
- **Simplest sync-safe persistence:** model the §3 payload with `kotlinx.serialization`
  and persist *that JSON* (e.g. one SQLDelight text row per profile, or
  multiplatform-settings) rather than a normalized relational schema. Guarantees a
  lossless round-trip with the server's opaque blob and keeps unknown fields.
- **Add** a Ktor client (not yet a dependency) for the §2 endpoints + auth header +
  token refresh.
- `ChecklistViewModel`/`DozenCatalog` move from a fixed 12 to the §4 master list driven
  by the active profile's `dietType`/`customServings` (§5).
- iOS launcher still needs an `initKoin()` call in `iOSApp.swift`.

## 9. Verification checklist before calling sync "done"

1. `toJsDateString()` passes all §6 test vectors (incl. zero-pad + leap day).
2. Round-trip: write a payload from KMP, load it in the **web app** (same account) —
   days/checks/diet/customServings all appear correctly, and vice-versa.
3. A blob written by the web app loads in KMP with **no dropped fields** (re-PUT it and
   confirm the server `updatedAt` round-trips without data loss).
4. Last-writer-wins matches: edit on web, then KMP pulls newer; edit on KMP, web pulls newer.
5. 401 on expired token triggers refresh, not a logout loop.
