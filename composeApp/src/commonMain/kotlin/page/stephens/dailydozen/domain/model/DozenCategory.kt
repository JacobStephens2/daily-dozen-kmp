package page.stephens.dailydozen.domain.model

/**
 * One of Dr. Greger's Daily Dozen categories.
 *
 * Pure domain model — no Compose, no platform dependencies. [target] is the
 * recommended number of daily servings.
 */
data class DozenCategory(
    val id: String,
    val name: String,
    val target: Int,
    val emoji: String,
)
