package page.stephens.dailydozen.data.sync

import page.stephens.dailydozen.data.SyncState
import page.stephens.dailydozen.data.model.Profile
import page.stephens.dailydozen.data.model.SyncBlob

/**
 * The safe-merge core (ADR-3 / §1). For a fixed (profile, date, category) the
 * stored value is a SET of checked serving indices, and the merge of two
 * observations is set union. Set union is a bounded join-semilattice — a
 * state-based G-Set CRDT — so it is COMMUTATIVE, ASSOCIATIVE, and IDEMPOTENT,
 * which makes N-way merges order-independent and replay-safe. The output is a
 * sorted, de-duped JSON array — structurally identical to a web-authored blob,
 * so this is a strict refinement of §7's LWW transport, not a fork of it.
 *
 * The one accepted trade (§1.2): union is grow-only, so an un-check on one
 * device while another still has it checked reappears after merge. Data LOSS is
 * catastrophic; an extra re-tappable check is trivial.
 */

/** Sorted, de-duped union of two index sets. The semilattice join. */
fun mergeIndexSets(a: List<Int>, b: List<Int>): List<Int> = (a + b).distinct().sorted()

private typealias DataMap = Map<String, Map<String, List<Int>>>

/** Per-cell union of two profiles' logs (the additive part that must never lose data). */
fun mergeDataMaps(a: DataMap, b: DataMap): DataMap {
    val days = a.keys + b.keys
    return days.associateWith { day ->
        val ac = a[day] ?: emptyMap()
        val bc = b[day] ?: emptyMap()
        (ac.keys + bc.keys).associateWith { cat ->
            mergeIndexSets(ac[cat] ?: emptyList(), bc[cat] ?: emptyList())
        }
    }
}

/** Result of merging two blobs, plus a heuristic contestability signal. */
data class MergeResult(
    val blob: SyncBlob,
    /**
     * Best-effort signal (GPT-5.5's contestability banner): true if, for some
     * cell, the server's set is a strict subset of local's — i.e., a remote
     * un-check that union deliberately did NOT apply. Heuristic (no 3-way base),
     * so it can also trip on a local-only addition; used only for a soft,
     * non-destructive "your devices differed; nothing was lost" notice.
     */
    val remoteDeletionsSuppressed: Boolean,
)

private fun mergeProfile(local: Profile, server: Profile, preferServerScalars: Boolean): Pair<Profile, Boolean> {
    var suppressed = false
    val days = local.data.keys + server.data.keys
    val mergedData = days.associateWith { day ->
        val lc = local.data[day] ?: emptyMap()
        val sc = server.data[day] ?: emptyMap()
        (lc.keys + sc.keys).associateWith { cat ->
            val la = (lc[cat] ?: emptyList()).toSet()
            val sa = (sc[cat] ?: emptyList()).toSet()
            if (sc.containsKey(cat) && (la - sa).isNotEmpty() && (sa - la).isEmpty()) suppressed = true
            mergeIndexSets(la.toList(), sa.toList())
        }
    }
    val scalars = if (preferServerScalars) server else local
    // Keep all unknown keys; the preferred side wins on a key collision.
    val unknown = if (preferServerScalars) local.unknown + server.unknown else server.unknown + local.unknown
    return scalars.copy(data = mergedData, unknown = unknown) to suppressed
}

/**
 * Merge two whole blobs: additive logs are union-merged (lossless); single-valued
 * profile scalars (name/color/dietType/customServings) and unknown-key collisions
 * resolve LWW toward [preferServerScalars]; unknown keys are otherwise unioned so
 * nothing is dropped (§1.2 forward-compat).
 */
fun mergeBlob(local: SyncBlob, server: SyncBlob, preferServerScalars: Boolean): MergeResult {
    var suppressed = false
    val ids = local.profiles.keys + server.profiles.keys
    val profiles = ids.associateWith { id ->
        val l = local.profiles[id]
        val s = server.profiles[id]
        when {
            l == null -> s!!
            s == null -> l
            else -> mergeProfile(l, s, preferServerScalars).also { if (it.second) suppressed = true }.first
        }
    }
    val unknown = if (preferServerScalars) local.unknown + server.unknown else server.unknown + local.unknown
    return MergeResult(SyncBlob(profiles, unknown), suppressed)
}

/** What a sync round should do, decided purely from local + server state. */
sealed interface SyncDecision {
    /** Nothing changed on either side. */
    data object Noop : SyncDecision

    /** Fresh device or server-only change with no local edits: take the server blob. */
    data class Adopt(val server: SyncBlob, val updatedAt: String?) : SyncDecision

    /** Only local changed (or server has nothing): PUT local as-is. */
    data class Push(val blob: SyncBlob) : SyncDecision

    /** Both diverged: PUT the union. */
    data class Merge(val merged: SyncBlob, val remoteDeletionsSuppressed: Boolean) : SyncDecision
}

/**
 * The sync state machine (ADR-3), as a pure function so it is unit-testable
 * without a network or DB. Encodes the data-loss mitigations directly:
 *  - D4: empty local is treated as "needs pull", NEVER an authoritative empty push.
 *  - D2: an existing device with unsynced edits MERGES, never adopts-over-local.
 *  - D3: a dirty device with a newer server MERGES before PUT, never blind-clobbers.
 */
fun decideSync(local: SyncState, serverBlob: SyncBlob?, serverUpdatedAt: String?): SyncDecision {
    val localEmpty = local.blob.profiles.isEmpty()

    // D4: never push an empty local over the server; empty means "needs pull".
    if (localEmpty) return if (serverBlob != null) SyncDecision.Adopt(serverBlob, serverUpdatedAt) else SyncDecision.Noop

    if (serverBlob == null) return SyncDecision.Push(local.blob)

    val serverNewer = serverUpdatedAt != null &&
        (local.syncedUpdatedAt == null || serverUpdatedAt > local.syncedUpdatedAt)

    return when {
        local.dirty && serverNewer -> {
            val result = mergeBlob(local.blob, serverBlob, preferServerScalars = true)
            SyncDecision.Merge(result.blob, result.remoteDeletionsSuppressed)
        }
        local.dirty && !serverNewer -> SyncDecision.Push(local.blob)
        !local.dirty && serverNewer -> SyncDecision.Adopt(serverBlob, serverUpdatedAt)
        else -> SyncDecision.Noop
    }
}
