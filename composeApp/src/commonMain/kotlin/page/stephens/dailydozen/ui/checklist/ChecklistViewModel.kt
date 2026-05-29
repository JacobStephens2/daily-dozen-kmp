package page.stephens.dailydozen.ui.checklist

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import page.stephens.dailydozen.domain.DozenCatalog
import page.stephens.dailydozen.domain.model.CategoryProgress

/**
 * Holds the day's serving counts. For the first milestone this is purely
 * in-memory; a SQLDelight-backed repository replaces the counts map in a
 * later milestone without touching the UI.
 */
class ChecklistViewModel : ViewModel() {

    private var counts: Map<String, Int> = emptyMap()

    private val _state = MutableStateFlow(buildState())
    val state: StateFlow<ChecklistUiState> = _state.asStateFlow()

    fun increment(categoryId: String) = mutate(categoryId, +1)
    fun decrement(categoryId: String) = mutate(categoryId, -1)

    private fun mutate(categoryId: String, delta: Int) {
        val next = ((counts[categoryId] ?: 0) + delta).coerceAtLeast(0)
        counts = counts + (categoryId to next)
        _state.value = buildState()
    }

    private fun buildState(): ChecklistUiState {
        val progress = DozenCatalog.categories.map { category ->
            CategoryProgress(category, counts[category.id] ?: 0)
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
