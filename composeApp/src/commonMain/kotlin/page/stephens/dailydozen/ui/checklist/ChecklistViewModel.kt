package page.stephens.dailydozen.ui.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import page.stephens.dailydozen.data.DayProgressInput
import page.stephens.dailydozen.data.DozenRepository
import page.stephens.dailydozen.domain.DietPresets
import page.stephens.dailydozen.domain.DozenCatalog
import page.stephens.dailydozen.domain.model.CategoryProgress
import page.stephens.dailydozen.domain.todayKey

/**
 * Exposes today's checklist as reactive state backed by [DozenRepository].
 * Active categories and per-category targets derive from the active profile's
 * diet preset (§5), so switching diet on the web reflects here after a sync.
 * Serving counts persist across launches; the UI never touches the DB directly.
 */
class ChecklistViewModel(
    private val repository: DozenRepository,
) : ViewModel() {

    private val today: String = todayKey()

    val state: StateFlow<ChecklistUiState> = repository.dayFlow(today)
        .map(::buildState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = buildState(DayProgressInput("standard", null, emptyMap())),
        )

    fun increment(categoryId: String) = adjust(categoryId, +1)
    fun decrement(categoryId: String) = adjust(categoryId, -1)

    private fun adjust(categoryId: String, delta: Int) {
        val current = state.value.progress
            .firstOrNull { it.category.id == categoryId }?.count ?: 0
        val next = (current + delta).coerceAtLeast(0)
        viewModelScope.launch { repository.setCount(today, categoryId, next) }
    }

    private fun buildState(input: DayProgressInput): ChecklistUiState {
        val targets = DietPresets.targetsFor(input.dietType, input.customServings)
        val active = DozenCatalog.categories.filter { (targets[it.id] ?: 0) > 0 }
        val progress = active.map { category ->
            CategoryProgress(category, targets[category.id] ?: 0, input.counts[category.id] ?: 0)
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
