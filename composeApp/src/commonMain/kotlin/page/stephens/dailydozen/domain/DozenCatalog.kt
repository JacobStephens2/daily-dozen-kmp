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
        DozenCategory("beans", "Beans", "🫘", "½ c. cooked beans, ¼ c. hummus"),
        DozenCategory("protein", "Protein", "🥩", "3 oz lean meat, 1 egg, ½ c. beans"),
        DozenCategory("berries", "Berries", "🫐", "½ c. fresh or frozen, ¼ c. dried"),
        DozenCategory("other-fruits", "Other Fruits", "🍎", "1 medium fruit, ¼ c. dried fruit"),
        DozenCategory("greens", "Greens", "🥬", "1 c. raw, ½ c. cooked"),
        DozenCategory("cruciferous", "Cruciferous Vegetables", "🥦", "½ c. chopped, 1 tbsp horseradish"),
        DozenCategory("other-vegetables", "Other Vegetables", "🥕", "½ c. nonleafy vegetables"),
        DozenCategory("flaxseed", "Flaxseed", "🌾", "1 tbsp ground"),
        DozenCategory("nuts-seeds", "Nuts and Seeds", "🥜", "¼ c. nuts, 2 tbsp nut butter"),
        DozenCategory("herbs-spices", "Herbs and Spices", "🌿", "¼ tsp turmeric"),
        DozenCategory("whole-grains", "Whole Grains", "🌾", "½ c. hot cereal, 1 slice of bread"),
        DozenCategory("beverages", "Beverages", "💧", "60 oz per day"),
        DozenCategory("exercise", "Exercise", "🏃", "90 min. moderate or 40 min. vigorous"),
    )

    val byId: Map<String, DozenCategory> = categories.associateBy { it.id }
}
