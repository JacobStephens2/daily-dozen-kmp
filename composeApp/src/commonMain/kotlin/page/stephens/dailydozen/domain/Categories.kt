package page.stephens.dailydozen.domain

import page.stephens.dailydozen.domain.model.ActiveCategory
import page.stephens.dailydozen.domain.model.CategoryLink
import page.stephens.dailydozen.domain.model.DozenCategory

/** A named serving configuration; 0 servings means the category is excluded. */
data class Preset(
    val id: String,
    val name: String,
    val servings: Map<String, Int>,
)

/**
 * The canonical catalog, transcribed 1:1 from the web app's `js/categories.js`
 * (ground truth) — 13 categories with hyphenated IDs (incl. `protein`), the
 * exact serving-size text (with non-breaking spaces), NutritionFacts.org links,
 * and the five diet presets.
 */
object Categories {

    val all: List<DozenCategory> = listOf(
        DozenCategory("beans", "Beans", "🫘", "½ c. cooked beans, ¼ c. hummus",
            listOf("Black beans", "Chickpeas", "Lentils", "Hummus", "Edamame"),
            listOf(CategoryLink("https://nutritionfacts.org/topics/beans/"))),
        DozenCategory("protein", "Protein", "🥩", "3 oz lean meat, 1 egg, ½ c. beans",
            listOf("Chicken breast", "Fish", "Eggs", "Lean beef", "Tofu"),
            listOf(CategoryLink("https://nutritionfacts.org/topics/protein/"))),
        DozenCategory("berries", "Berries", "🫐", "½ c. fresh or frozen, ¼ c. dried",
            listOf("Blueberries", "Strawberries", "Raspberries", "Blackberries", "Cranberries"),
            listOf(CategoryLink("https://nutritionfacts.org/topics/berries/"))),
        DozenCategory("other-fruits", "Other Fruits", "🍎", "1 medium fruit, ¼ c. dried fruit",
            listOf("Apples", "Bananas", "Oranges", "Grapes", "Pineapple"),
            listOf(CategoryLink("https://nutritionfacts.org/topics/fruit/"))),
        DozenCategory("greens", "Greens", "🥬", "1 c. raw, ½ c. cooked",
            listOf("Spinach", "Kale", "Arugula", "Swiss chard", "Collard greens"),
            listOf(CategoryLink("https://nutritionfacts.org/topics/greens/"))),
        DozenCategory("cruciferous", "Cruciferous Vegetables", "🥦", "½ c. chopped, 1 tbsp horseradish",
            listOf("Broccoli", "Cauliflower", "Brussels sprouts", "Cabbage", "Kale"),
            listOf(CategoryLink("https://nutritionfacts.org/topics/cruciferous-vegetables/"))),
        DozenCategory("other-vegetables", "Other Vegetables", "🥕", "½ c. nonleafy vegetables",
            listOf("Carrots", "Bell peppers", "Tomatoes", "Cucumber", "Zucchini"),
            listOf(CategoryLink("https://nutritionfacts.org/topics/vegetables/"))),
        DozenCategory("flaxseed", "Flaxseed", "🌾", "1 tbsp ground",
            listOf("Ground flaxseed", "Flaxseed oil"),
            listOf(CategoryLink("https://nutritionfacts.org/topics/flax-seeds/"))),
        DozenCategory("nuts-seeds", "Nuts and Seeds", "🥜", "¼ c. nuts, 2 tbsp nut butter",
            listOf("Almonds", "Walnuts", "Chia seeds", "Pumpkin seeds", "Peanut butter"),
            listOf(
                CategoryLink("https://nutritionfacts.org/topics/nuts/", "Nuts"),
                CategoryLink("https://nutritionfacts.org/topics/seeds/", "Seeds"),
            )),
        DozenCategory("herbs-spices", "Herbs and Spices", "🌿", "¼ tsp turmeric",
            listOf("Turmeric", "Cinnamon", "Ginger", "Garlic", "Basil"),
            listOf(
                CategoryLink("https://nutritionfacts.org/topics/herbs/", "Herbs"),
                CategoryLink("https://nutritionfacts.org/topics/spices/", "Spices"),
            )),
        DozenCategory("whole-grains", "Whole Grains", "🌾", "½ c. hot cereal, 1 slice of bread",
            listOf("Oatmeal", "Brown rice", "Quinoa", "Whole wheat bread", "Barley"),
            listOf(CategoryLink("https://nutritionfacts.org/topics/grains/"))),
        DozenCategory("beverages", "Beverages", "💧", "60 oz per day",
            listOf("Water", "Green tea", "Hibiscus tea", "Herbal tea"),
            listOf(CategoryLink("https://nutritionfacts.org/topics/beverages/"))),
        DozenCategory("exercise", "Exercise", "🏃", "90 min. moderate or 40 min. vigorous",
            listOf("Walking", "Running", "Cycling", "Swimming", "Yoga"),
            listOf(CategoryLink("https://nutritionfacts.org/topics/exercise/"))),
    )

    private val byId: Map<String, DozenCategory> = all.associateBy { it.id }

    fun category(id: String): DozenCategory? = byId[id]

    /** Preset order matters for the Customize sheet — keep it insertion-ordered. */
    val presets: Map<String, Preset> = linkedMapOf(
        "standard" to Preset("standard", "Standard Daily Dozen", mapOf(
            "beans" to 3, "protein" to 0, "berries" to 1, "other-fruits" to 3, "greens" to 2,
            "cruciferous" to 1, "other-vegetables" to 2, "flaxseed" to 1, "nuts-seeds" to 1,
            "herbs-spices" to 1, "whole-grains" to 3, "beverages" to 5, "exercise" to 1)),
        "modified" to Preset("modified", "Modified", mapOf(
            "beans" to 0, "protein" to 2, "berries" to 1, "other-fruits" to 3, "greens" to 2,
            "cruciferous" to 1, "other-vegetables" to 2, "flaxseed" to 0, "nuts-seeds" to 1,
            "herbs-spices" to 1, "whole-grains" to 3, "beverages" to 5, "exercise" to 1)),
        "one-bean" to Preset("one-bean", "One Bean", mapOf(
            "beans" to 1, "protein" to 1, "berries" to 1, "other-fruits" to 3, "greens" to 2,
            "cruciferous" to 1, "other-vegetables" to 2, "flaxseed" to 0, "nuts-seeds" to 1,
            "herbs-spices" to 1, "whole-grains" to 3, "beverages" to 5, "exercise" to 1)),
        "one-bean-two-protein" to Preset("one-bean-two-protein", "One Bean + Two Protein", mapOf(
            "beans" to 1, "protein" to 2, "berries" to 1, "other-fruits" to 3, "greens" to 2,
            "cruciferous" to 1, "other-vegetables" to 2, "flaxseed" to 0, "nuts-seeds" to 1,
            "herbs-spices" to 1, "whole-grains" to 3, "beverages" to 5, "exercise" to 1)),
        "one-bean-two-protein-one-flax" to Preset("one-bean-two-protein-one-flax", "One Bean + Two Protein + Flax", mapOf(
            "beans" to 1, "protein" to 2, "berries" to 1, "other-fruits" to 3, "greens" to 2,
            "cruciferous" to 1, "other-vegetables" to 2, "flaxseed" to 1, "nuts-seeds" to 1,
            "herbs-spices" to 1, "whole-grains" to 3, "beverages" to 5, "exercise" to 1)),
    )

    /** Effective serving map for a profile: explicit custom servings, else the preset. */
    fun servingsFor(dietType: String, customServings: Map<String, Int>?): Map<String, Int> =
        customServings ?: (presets[dietType] ?: presets.getValue("standard")).servings

    /** Catalog order, filtered to categories with a positive serving target. */
    fun activeCategories(servings: Map<String, Int>): List<ActiveCategory> =
        all.filter { (servings[it.id] ?: 0) > 0 }
            .map { ActiveCategory(it, servings.getValue(it.id)) }
}
