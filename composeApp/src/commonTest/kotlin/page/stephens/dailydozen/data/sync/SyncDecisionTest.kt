package page.stephens.dailydozen.data.sync

import page.stephens.dailydozen.data.SyncState
import page.stephens.dailydozen.data.model.Profile
import page.stephens.dailydozen.data.model.SyncBlob
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The sync state machine (ADR-3) and its data-loss mitigations (D2/D3/D4),
 * tested purely without network or DB.
 */
class SyncDecisionTest {

    private fun blob(vararg beans: Int) = SyncBlob(
        profiles = mapOf(
            "user" to Profile(data = mapOf("Thu Jan 16 2025" to mapOf("beans" to beans.toList()))),
        ),
    )

    private val empty = SyncBlob.empty()

    @Test
    fun freshDeviceAdoptsServer() {
        val local = SyncState(empty, syncedUpdatedAt = null, dirty = false)
        val d = decideSync(local, blob(0, 1), "2025-01-16 00:00:00")
        assertIs<SyncDecision.Adopt>(d)
    }

    @Test
    fun emptyLocalNeverPushesEvenIfDirty_D4() {
        // Empty local must be treated as "needs pull", never an authoritative empty push.
        val local = SyncState(empty, syncedUpdatedAt = "2025-01-15 00:00:00", dirty = true)
        assertIs<SyncDecision.Adopt>(decideSync(local, blob(0), "2025-01-16 00:00:00"))
        assertIs<SyncDecision.Noop>(decideSync(local, null, null))
    }

    @Test
    fun localDataServerNullPushes() {
        val local = SyncState(blob(0, 1), syncedUpdatedAt = null, dirty = true)
        assertIs<SyncDecision.Push>(decideSync(local, null, null))
    }

    @Test
    fun bothDivergedMergesAndLosesNothing_D2_D3() {
        // Local dirty with [0,1]; server newer with [2]. Must merge to [0,1,2].
        val local = SyncState(blob(0, 1), syncedUpdatedAt = "2025-01-16 00:00:00", dirty = true)
        val d = decideSync(local, blob(2), "2025-01-16 09:00:00")
        assertIs<SyncDecision.Merge>(d)
        val merged = d.merged.profiles.getValue("user").data.getValue("Thu Jan 16 2025").getValue("beans")
        assertEquals(listOf(0, 1, 2), merged)
    }

    @Test
    fun dirtyButServerNotNewerPushes() {
        val local = SyncState(blob(0, 1), syncedUpdatedAt = "2025-01-16 09:00:00", dirty = true)
        assertIs<SyncDecision.Push>(decideSync(local, blob(2), "2025-01-16 00:00:00"))
    }

    @Test
    fun cleanLocalServerNewerAdopts() {
        val local = SyncState(blob(0), syncedUpdatedAt = "2025-01-16 00:00:00", dirty = false)
        assertIs<SyncDecision.Adopt>(decideSync(local, blob(0, 1), "2025-01-16 09:00:00"))
    }

    @Test
    fun nothingChangedIsNoop() {
        val local = SyncState(blob(0), syncedUpdatedAt = "2025-01-16 09:00:00", dirty = false)
        assertIs<SyncDecision.Noop>(decideSync(local, blob(0), "2025-01-16 09:00:00"))
    }

    @Test
    fun mergePrefersServerScalarsWhenServerNewer() {
        val local = SyncState(
            SyncBlob(mapOf("user" to Profile(dietType = "standard", data = mapOf("d" to mapOf("beans" to listOf(0)))))),
            syncedUpdatedAt = "2025-01-16 00:00:00",
            dirty = true,
        )
        val server = SyncBlob(mapOf("user" to Profile(dietType = "modified", data = mapOf("d" to mapOf("beans" to listOf(1))))))
        val d = decideSync(local, server, "2025-01-16 09:00:00")
        assertIs<SyncDecision.Merge>(d)
        assertEquals("modified", d.merged.profiles.getValue("user").dietType) // server newer wins scalar
        assertTrue(d.merged.profiles.getValue("user").data.getValue("d").getValue("beans").containsAll(listOf(0, 1)))
    }
}
