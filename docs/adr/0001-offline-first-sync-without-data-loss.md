# ADR-0001 — Offline-first sync without data loss against a last-writer-wins opaque-blob backend

**Status:** Accepted · **Date:** 2026-06-08
**Context:** Daily Dozen (Compose Multiplatform) added account sign-in and
cross-device sync against the *same* backend as the existing web app — a backend
the client does not control.

This is the one published, copyable artifact for this feature. It is engineering
pedagogy (offline-first / CRDT / KMP), **not** a safe-AI-automation pattern, so it
does not count toward any safe-AI pedagogy quota.

---

## Context

The server stores each user's data as a **single opaque JSON blob**: it does no
schema validation and no field-level merge — it just `JSON.parse`/`JSON.stringify`.
Its sync rule is whole-blob **last-writer-wins (LWW)** with a pull-on-login that
**overwrites local state**.

Adopting LWW verbatim re-creates a real outage a sibling app suffered: a stale or
empty local store (a reinstall, a background eviction, an expired session) pulls
the server blob and clobbers unsynced local edits; symmetrically, a device with
newer local edits but an older timestamp pushes and clobbers the server. The hard
requirement here is **"no silent data loss, ever."**

We do not control the server, so we cannot add per-field merge endpoints or
version vectors. We must reconcile a destructive transport with a
never-lose-data requirement, entirely on the client.

## The insight

The conflicting field is not a scalar — it is a **set**. In the blob,
`data[dateKey][categoryId]` is an array of *checked serving indices*. The merge
of two observations of the same cell is therefore **set union**:

```
mergeCell(a, b) = a ∪ b
```

Set union with these three properties is a **bounded join-semilattice** — exactly
a state-based **G-Set CRDT**:

- **Commutative** `a ∪ b = b ∪ a` — the result is independent of which device synced first.
- **Associative** `(a ∪ b) ∪ c = a ∪ (b ∪ c)` — N-way merges need no coordination or ordering.
- **Idempotent** `a ∪ a = a` — a duplicate pull or a retried PUT changes nothing (replay-safe).

The whole blob is a finite product of independent cells, so the cell-wise merge
lifts to the whole blob and inherits all three properties. (These laws are not
left to faith: they are enforced as property-based CI gates —
`MergeLawsTest` fails the build if a refactor breaks commutativity, associativity,
idempotence, or monotonicity.)

## Why this stays web-compatible (the deviation, stated honestly)

This deliberately deviates from the backend's "don't invent a smarter merge"
rule. It is safe because it is a **strict refinement of LWW, not a replacement**:

1. The merge output — a sorted, de-duped JSON array of integers — is structurally
   identical to a web-authored cell. The server stores it verbatim; the web app
   reads it as "which indices are checked." Neither can tell a merged array from a
   hand-tapped one.
2. Transport is unchanged: GET/PUT the whole blob, ordered by the server's
   `updatedAt`. The only change is that when *both* sides diverged we PUT the
   union instead of clobbering.
3. Single-valued profile scalars (`name`, `color`, `dietType`, `customServings`)
   genuinely can't be unioned, so they stay LWW by `updatedAt`. The merge is
   scoped to exactly the additive log where loss is unacceptable.
4. Unknown fields (anything the web adds that this client doesn't model) are
   carried through untouched, so forward-compat is preserved and a re-PUT never
   drops the web's data.

## The boundary (what merge does NOT fix)

Union is **grow-only**: an un-check on device A while device B still has it checked
will **reappear** after merge. For a daily food tracker this is the correct trade —
accidental data *loss* is catastrophic; an accidental extra check is trivially
re-tapped. A soft, non-destructive banner tells the user when this happened
("your devices differed; we kept them all"). Un-check propagation (a 2P-Set /
OR-Set with tombstones) is deferred until a real need appears.

## The other load-bearing rule: auth loss is not data loss

The merge math prevents *pull/push* clobbering. The companion rule prevents the
*auth* failure mode that actually caused the sibling outage:

> A 401 triggers exactly one silent token refresh and replay. If refresh itself
> fails, the app enters a "needs re-auth" state that **preserves all local data**.
> The token is cleared from secure storage **only** by explicit user logout — never
> by a 401 path. A CI guard fails the build if `TokenStore.clear()` is reachable
> from anywhere but logout.

Likewise: an **empty local store is treated as "needs pull," never as an
authoritative empty to push** — so a reinstall or a non-durable web reload can't
wipe the server.

## The reusable KMP pattern

- A bespoke `TokenStore` `expect`/`actual` (iOS Keychain / Android
  EncryptedSharedPreferences / Wasm localStorage) — explicit control over the JWT,
  with `clear()` as the single, logout-only removal path.
- A Ktor client whose Bearer plugin refreshes on 401 and replays, and on refresh
  failure surfaces 401 *without* touching the token or local data.
- The contract blob modeled with `kotlinx.serialization` custom serializers that
  preserve unknown keys at every level (lossless round-trip).

## Data dignity

- **Export:** the user can copy out their full data as JSON at any time.
- **Deletion:** local-only, and labeled honestly — the backend exposes no delete
  endpoint, so the UI says "this device only" rather than implying a server wipe.
- **Transparency:** a sync-status line always shows whether data is saved, syncing,
  offline, or awaiting re-auth — the antidote to silent failure.

## Consequences

One merge function and one "both diverged" branch in the sync state machine, plus
property tests — small, and the single highest-leverage safety decision in the
project. The same instincts (human-in-control, never destroy without consent,
contestable, auditable) transfer directly to higher-stakes systems; that
transferability is the honest justification for the pattern, not the tracker
itself.
