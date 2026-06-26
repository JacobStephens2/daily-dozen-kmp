package page.stephens.dailydozen.domain.model

/** A "more info" link for a category (NutritionFacts.org topic). */
data class CategoryLink(val label: String, val url: String)

/**
 * One Daily Dozen category from the master list (SYNC_CONTRACT.md §4).
 *
 * Pure domain model — no Compose, no platform dependencies. [id] is the exact
 * hyphenated id the web backend uses (sync breaks on any underscore); [name],
 * [emoji], [description] (sample serving sizes), and [infoLinks] are UI-only and
 * not part of the synced blob.
 */
data class DozenCategory(
    val id: String,
    val name: String,
    val emoji: String,
    /** Sample serving sizes, shown when the user expands the card. */
    val description: String = "",
    /** "More info" links (NutritionFacts.org), shown when the card is expanded. */
    val infoLinks: List<CategoryLink> = emptyList(),
)
