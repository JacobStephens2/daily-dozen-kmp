package page.stephens.dailydozen.domain

import page.stephens.dailydozen.domain.model.CategoryLink
import page.stephens.dailydozen.domain.model.DozenCategory

/**
 * The master list of 13 Daily Dozen categories, verbatim from SYNC_CONTRACT.md
 * §4 — exact hyphenated ids (incl. `protein`), in contract order. These ids are
 * the sync key: any underscore or omission silently diverges KMP from the web
 * backend. Which of these are *active*, and each one's daily target, depends on
 * the active diet preset (§5) — see [DietPresets]. NutritionFacts.org "more info"
 * links mirror the web app's CATEGORY_LINKS map.
 */
object DozenCatalog {
    private fun moreInfo(topic: String) =
        listOf(CategoryLink("More info", "https://nutritionfacts.org/topics/$topic/"))

    val categories: List<DozenCategory> = listOf(
        DozenCategory("beans", "Beans", "🫘", "½ c. cooked beans, ¼ c. hummus", moreInfo("beans")),
        DozenCategory("protein", "Protein", "🥩", "3 oz lean meat, 1 egg, ½ c. beans", moreInfo("protein")),
        DozenCategory("berries", "Berries", "🫐", "½ c. fresh or frozen, ¼ c. dried", moreInfo("berries")),
        DozenCategory("other-fruits", "Other Fruits", "🍎", "1 medium fruit, ¼ c. dried fruit", moreInfo("fruit")),
        DozenCategory("greens", "Greens", "🥬", "1 c. raw, ½ c. cooked", moreInfo("greens")),
        DozenCategory("cruciferous", "Cruciferous Vegetables", "🥦", "½ c. chopped, 1 tbsp horseradish", moreInfo("cruciferous-vegetables")),
        DozenCategory("other-vegetables", "Other Vegetables", "🥕", "½ c. nonleafy vegetables", moreInfo("vegetables")),
        DozenCategory("flaxseed", "Flaxseed", "🌾", "1 tbsp ground", moreInfo("flax-seeds")),
        DozenCategory(
            "nuts-seeds", "Nuts and Seeds", "🥜", "¼ c. nuts, 2 tbsp nut butter",
            listOf(
                CategoryLink("Nuts", "https://nutritionfacts.org/topics/nuts/"),
                CategoryLink("Seeds", "https://nutritionfacts.org/topics/seeds/"),
            ),
        ),
        DozenCategory(
            "herbs-spices", "Herbs and Spices", "🌿", "¼ tsp turmeric",
            listOf(
                CategoryLink("Herbs", "https://nutritionfacts.org/topics/herbs/"),
                CategoryLink("Spices", "https://nutritionfacts.org/topics/spices/"),
            ),
        ),
        DozenCategory("whole-grains", "Whole Grains", "🌾", "½ c. hot cereal, 1 slice of bread", moreInfo("grains")),
        DozenCategory("beverages", "Beverages", "💧", "60 oz per day", moreInfo("beverages")),
        DozenCategory("exercise", "Exercise", "🏃", "90 min. moderate or 40 min. vigorous", moreInfo("exercise")),
    )

    val byId: Map<String, DozenCategory> = categories.associateBy { it.id }
}
