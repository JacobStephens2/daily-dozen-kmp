package page.stephens.dailydozen.domain

import page.stephens.dailydozen.domain.model.DozenCategory

/**
 * The canonical 12 Daily Dozen categories with their recommended serving
 * targets, per Dr. Greger's "How Not to Die". This is the in-memory source
 * the first-milestone skeleton renders; persistence is layered on later.
 */
object DozenCatalog {
    val categories: List<DozenCategory> = listOf(
        DozenCategory("beans", "Beans", target = 3, emoji = "🫘"),
        DozenCategory("berries", "Berries", target = 1, emoji = "🫐"),
        DozenCategory("other_fruits", "Other Fruits", target = 3, emoji = "🍎"),
        DozenCategory("cruciferous", "Cruciferous Vegetables", target = 1, emoji = "🥦"),
        DozenCategory("greens", "Greens", target = 2, emoji = "🥬"),
        DozenCategory("other_veg", "Other Vegetables", target = 2, emoji = "🥕"),
        DozenCategory("flaxseeds", "Flaxseeds", target = 1, emoji = "🌾"),
        DozenCategory("nuts_seeds", "Nuts & Seeds", target = 1, emoji = "🥜"),
        DozenCategory("herbs_spices", "Herbs & Spices", target = 1, emoji = "🌶️"),
        DozenCategory("whole_grains", "Whole Grains", target = 3, emoji = "🌾"),
        DozenCategory("beverages", "Beverages", target = 5, emoji = "💧"),
        DozenCategory("exercise", "Exercise", target = 1, emoji = "🏃"),
    )
}
