package page.stephens.dailydozen.domain

/**
 * A diet preset: a map of categoryId -> daily serving target. A target of 0
 * means the category is excluded for that diet. Verbatim from SYNC_CONTRACT.md
 * §5 — the keys are the §4 hyphenated ids.
 */
data class DietPreset(
    val id: String,
    val label: String,
    val servings: Map<String, Int>,
)

/**
 * The 5 diet presets (§5) plus the rules for turning a profile's `dietType` +
 * nullable `customServings` into effective per-category targets and the active
 * category list. Unknown/missing `dietType` falls back to `standard` (§5).
 */
object DietPresets {

    // Order matches §4 so the maps read like the contract's table rows.
    private fun preset(id: String, label: String, vararg targets: Int): DietPreset {
        val ids = DozenCatalog.categories.map { it.id }
        require(targets.size == ids.size) { "preset $id needs ${ids.size} targets, got ${targets.size}" }
        return DietPreset(id, label, ids.zip(targets.toList()).toMap())
    }

    //                                            beans protein berries o-fruits greens cruci o-veg flax nuts herbs grains bev exer
    val standard = preset("standard", "Standard Daily Dozen",        3,  0,  1,  3,  2,  1,  2,  1,  1,  1,  3,  5,  1)
    val modified = preset("modified", "Modified",                    0,  2,  1,  3,  2,  1,  2,  0,  1,  1,  3,  5,  1)
    val oneBean = preset("one-bean", "One Bean",                     1,  1,  1,  3,  2,  1,  2,  0,  1,  1,  3,  5,  1)
    val oneBeanTwoProtein = preset("one-bean-two-protein", "One Bean + Two Protein",
                                                                     1,  2,  1,  3,  2,  1,  2,  0,  1,  1,  3,  5,  1)
    val oneBeanTwoProteinOneFlax = preset("one-bean-two-protein-one-flax", "One Bean + Two Protein + Flax",
                                                                     1,  2,  1,  3,  2,  1,  2,  1,  1,  1,  3,  5,  1)

    val all: List<DietPreset> = listOf(standard, modified, oneBean, oneBeanTwoProtein, oneBeanTwoProteinOneFlax)

    private val byId: Map<String, DietPreset> = all.associateBy { it.id }

    /** The preset for [dietType], falling back to `standard` for unknown/null (§5). */
    fun byId(dietType: String?): DietPreset = byId[dietType] ?: standard

    /**
     * Effective per-category targets for a profile: the preset for [dietType],
     * with [customServings] overriding it per category when non-null (§3). A
     * partial customServings map overrides only the categories it names.
     */
    fun targetsFor(dietType: String?, customServings: Map<String, Int>?): Map<String, Int> {
        val base = byId(dietType).servings
        return if (customServings == null) base else base + customServings
    }

    /** Category ids with target > 0, in catalog order (`getActiveCategories()`, §5). */
    fun activeCategoryIds(dietType: String?, customServings: Map<String, Int>?): List<String> {
        val targets = targetsFor(dietType, customServings)
        return DozenCatalog.categories.map { it.id }.filter { (targets[it] ?: 0) > 0 }
    }
}
