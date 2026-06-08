package page.stephens.dailydozen.ui.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import page.stephens.dailydozen.data.DozenRepository
import page.stephens.dailydozen.domain.DietPresets
import page.stephens.dailydozen.domain.DozenCatalog
import page.stephens.dailydozen.domain.model.CategoryProgress
import page.stephens.dailydozen.domain.todayKey

/**
 * Exposes today's checklist as reactive state backed by [DozenRepository].
 * Serving counts persist across launches; the UI never touches the database
 * directly.
 */
class ChecklistViewModel(
    private val repository: DozenRepository,
) : ViewModel() {

    // Interim until M1 wires the active profile's dietType: render the Standard
    // preset. Active categories = preset entries with target > 0 (§5).
    private val targets: Map<String, Int> = DietPresets.targetsFor("standard", null)
    private val activeCategories = DozenCatalog.categories.filter { (targets[it.id] ?: 0) > 0 }

    private val today: String = todayKey()

    val state: StateFlow<ChecklistUiState> = repository.countsForDay(today)
        .map(::buildState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = buildState(emptyMap()),
        )

    fun increment(categoryId: String) = adjust(categoryId, +1)
    fun decrement(categoryId: String) = adjust(categoryId, -1)

    private fun adjust(categoryId: String, delta: Int) {
        val current = state.value.progress
            .firstOrNull { it.category.id == categoryId }?.count ?: 0
        val next = (current + delta).coerceAtLeast(0)
        viewModelScope.launch { repository.setCount(today, categoryId, next) }
    }

    private fun buildState(counts: Map<String, Int>): ChecklistUiState {
        val progress = activeCategories.map { category ->
            CategoryProgress(category, targets[category.id] ?: 0, counts[category.id] ?: 0)
        }
        return ChecklistUiState(
            progress = progress,
            completedCount = progress.count { it.isComplete },
            totalCount = progress.size,
        )
    }
}

data class ChecklistUiState(
    val progress: List<CategoryProgress>,
    val completedCount: Int,
    val totalCount: Int,
)
