package page.stephens.dailydozen.domain.model

/**
 * One Daily Dozen category from the master list (SYNC_CONTRACT.md §4).
 *
 * Pure domain model — no Compose, no platform dependencies. [id] is the exact
 * hyphenated id the web backend uses (sync breaks on any underscore); [name]
 * and [emoji] are UI-only and not part of the synced blob. Per-day serving
 * targets are NOT a property of the category — they come from the active diet
 * preset (§5), so they live in [page.stephens.dailydozen.domain.DietPresets].
 */
data class DozenCategory(
    val id: String,
    val name: String,
    val emoji: String,
)
