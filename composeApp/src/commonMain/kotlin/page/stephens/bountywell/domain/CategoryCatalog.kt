package page.stephens.bountywell.domain

import page.stephens.bountywell.domain.model.FoodCategory

/**
 * The master list of 13 food categories, verbatim from SYNC_CONTRACT.md §4 —
 * exact hyphenated ids (incl. `protein`), in contract order. These ids are the
 * sync key: any underscore or omission silently diverges KMP from the web
 * backend. Which of these are *active*, and each one's daily target, depends on
 * the active diet preset (§5) — see [DietPresets].
 */
object CategoryCatalog {
    val categories: List<FoodCategory> = listOf(
        FoodCategory("beans", "Beans", "🫘", "½ c. cooked beans, ¼ c. hummus"),
        FoodCategory("protein", "Protein", "🥩", "3 oz lean meat, 1 egg, ½ c. beans"),
        FoodCategory("berries", "Berries", "🫐", "½ c. fresh or frozen, ¼ c. dried"),
        FoodCategory("other-fruits", "Other Fruits", "🍎", "1 medium fruit, ¼ c. dried fruit"),
        FoodCategory("greens", "Greens", "🥬", "1 c. raw, ½ c. cooked"),
        FoodCategory("cruciferous", "Cruciferous Vegetables", "🥦", "½ c. chopped, 1 tbsp horseradish"),
        FoodCategory("other-vegetables", "Other Vegetables", "🥕", "½ c. nonleafy vegetables"),
        FoodCategory("flaxseed", "Flaxseed", "🌾", "1 tbsp ground"),
        FoodCategory("nuts-seeds", "Nuts and Seeds", "🥜", "¼ c. nuts, 2 tbsp nut butter"),
        FoodCategory("herbs-spices", "Herbs and Spices", "🌿", "¼ tsp turmeric"),
        FoodCategory("whole-grains", "Whole Grains", "🌾", "½ c. hot cereal, 1 slice of bread"),
        FoodCategory("beverages", "Beverages", "💧", "60 oz per day"),
        FoodCategory("exercise", "Exercise", "🏃", "90 min. moderate or 40 min. vigorous"),
    )

    val byId: Map<String, FoodCategory> = categories.associateBy { it.id }
}
