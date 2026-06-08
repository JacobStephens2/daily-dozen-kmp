package page.stephens.dailydozen.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * ADR-4 — the count -> indices migration must be information-preserving,
 * id-correcting (underscore -> hyphen), date-correcting (ISO -> JS dateKey),
 * and idempotent.
 */
class LegacyMigrationTest {

    @Test
    fun expandsCountToIndexSetAndFixesIdsAndDates() {
        val rows = listOf(
            LegacyServing("2025-01-16", "beans", 3),
            LegacyServing("2025-01-16", "other_fruits", 1), // underscored legacy id
            LegacyServing("2025-01-16", "nuts_seeds", 2),
        )
        val blob = migrateCountsToBlob(rows)
        val day = blob.profiles.getValue("user").data.getValue("Thu Jan 16 2025")

        assertEquals(listOf(0, 1, 2), day.getValue("beans"))
        assertEquals(listOf(0), day.getValue("other-fruits")) // remapped id
        assertEquals(listOf(0, 1), day.getValue("nuts-seeds")) // remapped id
        assertEquals("standard", blob.profiles.getValue("user").dietType)
    }

    @Test
    fun zeroAndNegativeCountsAreDropped() {
        val blob = migrateCountsToBlob(
            listOf(
                LegacyServing("2025-01-16", "beans", 0),
                LegacyServing("2025-01-16", "greens", -1),
            ),
        )
        assertTrue(blob.profiles.isEmpty() || blob.profiles.getValue("user").data.isEmpty())
    }

    @Test
    fun emptyInputProducesEmptyBlob() {
        assertTrue(migrateCountsToBlob(emptyList()).profiles.isEmpty())
    }

    @Test
    fun isIdempotentOnRepeatedInput() {
        val rows = listOf(LegacyServing("2025-07-04", "whole_grains", 2))
        val once = migrateCountsToBlob(rows)
        val twice = migrateCountsToBlob(rows)
        assertEquals(once, twice)
        assertEquals(
            listOf(0, 1),
            once.profiles.getValue("user").data.getValue("Fri Jul 04 2025").getValue("whole-grains"),
        )
    }
}
