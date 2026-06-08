package page.stephens.dailydozen.domain

import page.stephens.dailydozen.domain.model.DozenCategory

/**
 * The master list of 13 Daily Dozen categories, verbatim from SYNC_CONTRACT.md
 * §4 — exact hyphenated ids (incl. `protein`), in contract order. These ids are
 * the sync key: any underscore or omission silently diverges KMP from the web
 * backend. Which of these are *active*, and each one's daily target, depends on
 * the active diet preset (§5) — see [DietPresets].
 */
object DozenCatalog {
    val categories: List<DozenCategory> = listOf(
        DozenCategory("beans", "Beans", emoji = "🫘"),
        DozenCategory("protein", "Protein", emoji = "🥩"),
        DozenCategory("berries", "Berries", emoji = "🫐"),
        DozenCategory("other-fruits", "Other Fruits", emoji = "🍎"),
        DozenCategory("greens", "Greens", emoji = "🥬"),
        DozenCategory("cruciferous", "Cruciferous Vegetables", emoji = "🥦"),
        DozenCategory("other-vegetables", "Other Vegetables", emoji = "🥕"),
        DozenCategory("flaxseed", "Flaxseed", emoji = "🌾"),
        DozenCategory("nuts-seeds", "Nuts and Seeds", emoji = "🥜"),
        DozenCategory("herbs-spices", "Herbs and Spices", emoji = "🌿"),
        DozenCategory("whole-grains", "Whole Grains", emoji = "🌾"),
        DozenCategory("beverages", "Beverages", emoji = "💧"),
        DozenCategory("exercise", "Exercise", emoji = "🏃"),
    )

    val byId: Map<String, DozenCategory> = categories.associateBy { it.id }
}
