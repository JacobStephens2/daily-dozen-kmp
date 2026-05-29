package page.stephens.dailydozen.ui.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import page.stephens.dailydozen.data.DozenRepository
import page.stephens.dailydozen.domain.DozenCatalog
import page.stephens.dailydozen.domain.model.CategoryProgress

/**
 * Exposes today's checklist as reactive state backed by [DozenRepository].
 * Serving counts persist across launches; the UI never touches the database
 * directly.
 */
class ChecklistViewModel(
    private val repository: DozenRepository,
) : ViewModel() {

    private val today: String =
        Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

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
