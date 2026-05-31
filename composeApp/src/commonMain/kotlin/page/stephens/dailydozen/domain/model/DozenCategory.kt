package page.stephens.dailydozen.domain.model

/**
 * One Daily Dozen category definition (the catalog entry — not per-day state).
 * [description] is the serving-size example text; [links] are NutritionFacts.org
 * topic links (one entry, or two for "Nuts and Seeds" / "Herbs and Spices").
 */
data class DozenCategory(
    val id: String,
    val name: String,
    val icon: String,
    val description: String,
    val examples: List<String>,
    val links: List<CategoryLink> = emptyList(),
)

/** A NutritionFacts.org link. [label] null means render the category name. */
data class CategoryLink(
    val url: String,
    val label: String? = null,
)

/** A category paired with its active serving target for the current profile. */
data class ActiveCategory(
    val category: DozenCategory,
    val servings: Int,
)
