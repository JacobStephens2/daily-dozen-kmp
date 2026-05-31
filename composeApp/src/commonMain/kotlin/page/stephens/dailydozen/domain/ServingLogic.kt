package page.stephens.dailydozen.domain

/**
 * Pure port of the web app's `js/checkbox.js` "fill-to-left" tap behavior,
 * operating on the set of checked serving indices for one category instead of
 * the DOM. Tapping a serving box yields the new set of checked indices.
 *
 * Behavior (matching the original exactly):
 *  - Tapping an unchecked box with no checked box to its right → fill 0..index.
 *  - Tapping an unchecked box that has a checked box to its right → clear it and
 *    everything to its right (keep only indices < index).
 *  - Tapping the leftmost checked box → clear the category.
 *  - Tapping the rightmost checked box → clear the category.
 *  - Tapping a middle checked box that has checked boxes to its right → keep
 *    0..index, drop everything to the right.
 */
object ServingLogic {

    fun toggle(current: Set<Int>, target: Int, index: Int): Set<Int> {
        val willCheck = index !in current
        return if (willCheck) fillToLeft(current, target, index)
        else handleUncheck(current, target, index)
    }

    private fun fillToLeft(current: Set<Int>, target: Int, index: Int): Set<Int> {
        val hasCheckedRight = (index + 1 until target).any { it in current }
        return if (hasCheckedRight) {
            current.filter { it < index }.toSet()
        } else {
            current + (0..index)
        }
    }

    private fun handleUncheck(current: Set<Int>, target: Int, index: Int): Set<Int> {
        val leftmost = current.minOrNull() ?: return current
        val rightmost = current.maxOrNull() ?: return current
        return when {
            index == leftmost -> emptySet()
            index == rightmost -> current.filter { it > index }.toSet() // -> empty
            else -> {
                val hasCheckedRight = (index + 1 until target).any { it in current }
                if (hasCheckedRight) current.filter { it <= index }.toSet()
                else current - index
            }
        }
    }
}
