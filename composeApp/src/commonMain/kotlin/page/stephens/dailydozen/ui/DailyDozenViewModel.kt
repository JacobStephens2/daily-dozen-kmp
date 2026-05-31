package page.stephens.dailydozen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.minus
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.random.Random
import page.stephens.dailydozen.data.DozenRepository
import page.stephens.dailydozen.domain.Categories
import page.stephens.dailydozen.domain.DateKeys
import page.stephens.dailydozen.domain.Devotional
import page.stephens.dailydozen.domain.HistoryCalc
import page.stephens.dailydozen.domain.HistoryMonth
import page.stephens.dailydozen.domain.model.DataPayload
import page.stephens.dailydozen.domain.model.DozenCategory
import page.stephens.dailydozen.domain.model.ExportEnvelope
import page.stephens.dailydozen.domain.model.LocalState
import page.stephens.dailydozen.domain.model.ProfileData
import page.stephens.dailydozen.net.AuthManager
import page.stephens.dailydozen.net.NetworkMonitor

/** A category with its target and the currently-checked serving indices. */
data class CategoryRow(
    val category: DozenCategory,
    val servings: Int,
    val checked: Set<Int>,
)

data class ProfileChip(
    val id: String,
    val name: String,
    val color: String,
    val active: Boolean,
)

data class DozenUiState(
    val date: LocalDate,
    val dateKey: String,
    val dateLabel: String,
    val isToday: Boolean,
    val canGoNext: Boolean,
    val rows: List<CategoryRow>,
    val completed: Int,
    val total: Int,
    val profiles: List<ProfileChip>,
    val currentProfileId: String,
    val headerColor: String,
    val dietType: String,
    val servings: Map<String, Int>,
    val email: String?,
    val lastSync: String?,
)

/**
 * The single app-wide ViewModel. Combines the persisted [DozenRepository] state
 * with the currently-viewed date to drive every screen and modal, and exposes
 * the sync ([authManager]) and connectivity layers.
 */
class DailyDozenViewModel(
    val repository: DozenRepository,
    val authManager: AuthManager,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val viewedDate = MutableStateFlow(DateKeys.today())

    /** Emits the verse to show when the day reaches 100%, else null. */
    val celebration = MutableStateFlow<Devotional.Verse?>(null)

    val isLoggedIn: StateFlow<Boolean> = authManager.isLoggedIn
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    init {
        viewModelScope.launch {
            repository.ensureLoaded()
            authManager.startupSync()
        }
    }

    val ui: StateFlow<DozenUiState> =
        combine(repository.state, viewedDate) { state, date -> buildUi(state, date) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = buildUi(repository.state.value, viewedDate.value),
            )

    private fun buildUi(state: LocalState, date: LocalDate): DozenUiState {
        val pid = state.currentProfile
        val profile = state.payload.profiles[pid] ?: ProfileData("You", "#38672a")
        val servings = Categories.servingsFor(profile.dietType, profile.customServings)
        val active = Categories.activeCategories(servings)
        val dateKey = DateKeys.toDateString(date)
        val day = profile.data[dateKey].orEmpty()
        val rows = active.map { ac ->
            CategoryRow(ac.category, ac.servings, day[ac.category.id].orEmpty().toSet())
        }
        val today = DateKeys.today()
        return DozenUiState(
            date = date,
            dateKey = dateKey,
            dateLabel = DateKeys.longLabel(date),
            isToday = date == today,
            canGoNext = date < today,
            rows = rows,
            completed = rows.sumOf { it.checked.size },
            total = rows.sumOf { it.servings },
            profiles = state.payload.profiles.map { (id, p) ->
                ProfileChip(id, p.name, p.color, id == pid)
            },
            currentProfileId = pid,
            headerColor = profile.color,
            dietType = profile.dietType,
            servings = servings,
            email = state.email,
            lastSync = state.lastSync,
        )
    }

    // ---- Serving edits ----

    fun toggleServing(categoryId: String, index: Int) {
        val date = viewedDate.value
        viewModelScope.launch {
            repository.toggleServing(DateKeys.toDateString(date), categoryId, index)
            maybeCelebrate(date)
        }
    }

    private fun maybeCelebrate(date: LocalDate) {
        if (date != DateKeys.today()) return
        val state = buildUi(repository.state.value, date)
        if (state.total > 0 && state.completed >= state.total && !repository.celebrationShown(state.dateKey)) {
            celebration.value = Devotional.verses[Random.nextInt(Devotional.verses.size)]
            viewModelScope.launch { repository.markCelebrationShown(state.dateKey) }
        }
    }

    fun dismissCelebration() {
        celebration.value = null
    }

    // ---- Day navigation ----

    fun prevDay() {
        viewedDate.value = viewedDate.value.minus(1, DateTimeUnit.DAY)
    }

    fun nextDay() {
        val next = viewedDate.value.plus(1, DateTimeUnit.DAY)
        if (next <= DateKeys.today()) viewedDate.value = next
    }

    fun goToday() {
        viewedDate.value = DateKeys.today()
    }

    fun navigateToDate(date: LocalDate) {
        if (date <= DateKeys.today()) viewedDate.value = date
    }

    // ---- Reset / categories / profiles ----

    fun resetDay() {
        val date = viewedDate.value
        viewModelScope.launch { repository.resetDay(DateKeys.toDateString(date)) }
    }

    fun applyPreset(presetId: String) {
        viewModelScope.launch { repository.applyPreset(presetId) }
    }

    fun setCustomServings(servings: Map<String, Int>) {
        viewModelScope.launch { repository.setCustomServings(servings) }
    }

    fun switchProfile(profileId: String) {
        viewModelScope.launch { repository.switchProfile(profileId) }
    }

    fun renameProfile(profileId: String, name: String) {
        viewModelScope.launch { repository.renameProfile(profileId, name) }
    }

    /** History calendar data for the current profile and given month (1-12). */
    fun historyMonth(year: Int, month: Int): HistoryMonth {
        val state = repository.state.value
        val profile = state.payload.profiles[state.currentProfile]
            ?: ProfileData("You", "#38672a")
        return HistoryCalc.build(profile, year, month, DateKeys.today())
    }

    // ---- Export / import ----

    fun exportFileName(): String = "daily-dozen-${DateKeys.today()}.json"

    fun exportJson(): String {
        val envelope = ExportEnvelope(
            exportDate = Clock.System.now().toString(),
            profiles = repository.currentPayload().profiles,
        )
        return exportJsonFormat.encodeToString(ExportEnvelope.serializer(), envelope)
    }

    fun importJson(content: String) {
        viewModelScope.launch {
            runCatching {
                val obj = importJsonFormat.parseToJsonElement(content).jsonObject
                val profilesEl = obj["profiles"] ?: return@runCatching
                val profiles = importJsonFormat
                    .decodeFromJsonElement(profilesMapSerializer, profilesEl)
                repository.replacePayload(DataPayload(profiles), null)
            }
        }
    }

    private companion object {
        val exportJsonFormat = Json { prettyPrint = true; encodeDefaults = true }
        val importJsonFormat = Json { ignoreUnknownKeys = true }
        val profilesMapSerializer =
            MapSerializer(String.serializer(), ProfileData.serializer())
    }
}
