package page.stephens.dailydozen.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * SYNC_CONTRACT.md §4/§5 — the catalog and presets are the sync key. These
 * guard the most common silent-divergence bugs (failure mode D5): an underscore
 * in an id, a missing `protein`, or a preset that doesn't cover all 13 ids.
 */
class CatalogTest {

    private val expectedIds = listOf(
        "beans", "protein", "berries", "other-fruits", "greens", "cruciferous",
        "other-vegetables", "flaxseed", "nuts-seeds", "herbs-spices",
        "whole-grains", "beverages", "exercise",
    )

    @Test
    fun has13ExactHyphenatedIds() {
        assertEquals(expectedIds, DozenCatalog.categories.map { it.id })
    }

    @Test
    fun noUnderscoresInAnyId() {
        DozenCatalog.categories.forEach {
            assertFalse(it.id.contains('_'), "id '${it.id}' must be hyphenated, not underscored")
        }
    }

    @Test
    fun includesProtein() {
        assertTrue(DozenCatalog.byId.containsKey("protein"))
    }

    @Test
    fun allFivePresetsCoverAll13Ids() {
        assertEquals(5, DietPresets.all.size)
        DietPresets.all.forEach { preset ->
            assertEquals(
                expectedIds.toSet(),
                preset.servings.keys,
                "preset '${preset.id}' must define a target for every category",
            )
        }
    }

    @Test
    fun standardPresetMatchesContractTable() {
        // §5 Standard row.
        val s = DietPresets.standard.servings
        assertEquals(3, s["beans"]); assertEquals(0, s["protein"]); assertEquals(1, s["berries"])
        assertEquals(3, s["other-fruits"]); assertEquals(2, s["greens"]); assertEquals(1, s["cruciferous"])
        assertEquals(2, s["other-vegetables"]); assertEquals(1, s["flaxseed"]); assertEquals(1, s["nuts-seeds"])
        assertEquals(1, s["herbs-spices"]); assertEquals(3, s["whole-grains"]); assertEquals(5, s["beverages"])
        assertEquals(1, s["exercise"])
    }

    @Test
    fun standardExcludesProteinFromActiveCategories() {
        val active = DietPresets.activeCategoryIds("standard", null)
        assertFalse(active.contains("protein")) // target 0 in standard
        assertEquals(12, active.size)
    }

    @Test
    fun unknownDietTypeFallsBackToStandard() {
        assertEquals(DietPresets.standard, DietPresets.byId("nonsense"))
        assertEquals(DietPresets.standard, DietPresets.byId(null))
    }

    @Test
    fun customServingsOverridePresetPerCategory() {
        // A partial customServings overrides only named categories (§3).
        val targets = DietPresets.targetsFor("standard", mapOf("protein" to 2, "beans" to 0))
        assertEquals(2, targets["protein"]) // overridden
        assertEquals(0, targets["beans"]) // overridden
        assertEquals(1, targets["berries"]) // untouched preset value
    }
}
