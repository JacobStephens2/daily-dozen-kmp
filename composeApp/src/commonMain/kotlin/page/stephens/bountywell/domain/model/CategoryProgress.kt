package page.stephens.bountywell.domain.model

/**
 * A category paired with its diet-derived daily [target] and how many servings
 * have been logged today ([count]). [target] comes from the active diet preset
 * (§5), not the category itself.
 */
data class CategoryProgress(
    val category: FoodCategory,
    val target: Int,
    val count: Int,
) {
    val isComplete: Boolean get() = count >= target
    val remaining: Int get() = (target - count).coerceAtLeast(0)
}
