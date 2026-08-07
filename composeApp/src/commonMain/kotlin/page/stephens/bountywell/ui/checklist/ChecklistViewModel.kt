package page.stephens.bountywell.ui.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import page.stephens.bountywell.data.DayProgressInput
import page.stephens.bountywell.data.TrackerRepository
import page.stephens.bountywell.data.sync.SyncEngine
import page.stephens.bountywell.domain.DietPresets
import page.stephens.bountywell.domain.CategoryCatalog
import page.stephens.bountywell.domain.model.CategoryProgress
import page.stephens.bountywell.domain.todayKey

/**
 * Exposes today's checklist as reactive state backed by [TrackerRepository].
 * Active categories and per-category targets derive from the active profile's
 * diet preset (§5), so switching diet on the web reflects here after a sync.
 * Serving counts persist across launches; the UI never touches the DB directly.
 */
class ChecklistViewModel(
    private val repository: TrackerRepository,
    private val syncEngine: SyncEngine,
) : ViewModel() {

    private val today: String = todayKey()

    val state: StateFlow<ChecklistUiState> = repository.dayFlow(today)
        .map(::buildState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = buildState(DayProgressInput("standard", null, emptyMap())),
        )

    /** Set the absolute serving count for a category (from the serving chips). */
    fun setCount(categoryId: String, count: Int) {
        viewModelScope.launch {
            repository.setCount(today, categoryId, count.coerceAtLeast(0))
            // Debounced push (§7); a no-op when signed out (no token).
            syncEngine.requestSync()
        }
    }

    private fun buildState(input: DayProgressInput): ChecklistUiState {
        val targets = DietPresets.targetsFor(input.dietType, input.customServings)
        val active = CategoryCatalog.categories.filter { (targets[it.id] ?: 0) > 0 }
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
