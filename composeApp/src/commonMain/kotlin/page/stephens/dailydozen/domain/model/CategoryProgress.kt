package page.stephens.dailydozen.domain.model

/**
 * A category paired with how many servings have been logged today.
 * [done] caps display at the target; [count] is the raw logged amount.
 */
data class CategoryProgress(
    val category: DozenCategory,
    val count: Int,
) {
    val isComplete: Boolean get() = count >= category.target
    val remaining: Int get() = (category.target - count).coerceAtLeast(0)
}
