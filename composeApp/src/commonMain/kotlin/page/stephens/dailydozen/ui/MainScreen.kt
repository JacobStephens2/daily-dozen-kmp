package page.stephens.dailydozen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import page.stephens.dailydozen.domain.DateKeys
import page.stephens.dailydozen.domain.Devotional
import page.stephens.dailydozen.platform.pickJson
import page.stephens.dailydozen.platform.shareJson
import page.stephens.dailydozen.ui.components.CategoryCard
import page.stephens.dailydozen.ui.components.DateNavBar
import page.stephens.dailydozen.ui.components.DozenProgressBar
import page.stephens.dailydozen.ui.components.ProfileSelector
import page.stephens.dailydozen.ui.modals.AccountDialog
import page.stephens.dailydozen.ui.modals.BlessingDialog
import page.stephens.dailydozen.ui.modals.CelebrationDialog
import page.stephens.dailydozen.ui.modals.CustomizeDialog
import page.stephens.dailydozen.ui.modals.HistoryDialog
import page.stephens.dailydozen.ui.theme.DozenPalette

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(viewModel: DailyDozenViewModel = koinViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val celebration by viewModel.celebration.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    var showAccount by remember { mutableStateOf(false) }
    var showBlessing by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showCustomize by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<ProfileChip?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Daily Dozen Tracker", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DozenPalette.surface,
                    titleContentColor = DozenPalette.primary,
                ),
            )
        },
        containerColor = DozenPalette.surfaceContainerLow,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!isOnline) {
                item {
                    Text(
                        text = "You are offline — changes are saved locally",
                        style = MaterialTheme.typography.bodySmall,
                        color = DozenPalette.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                DateNavBar(
                    label = ui.dateLabel,
                    isToday = ui.isToday,
                    canGoNext = ui.canGoNext,
                    onPrev = viewModel::prevDay,
                    onNext = viewModel::nextDay,
                    onPickDate = { showDatePicker = true },
                    onToday = viewModel::goToday,
                )
            }
            item {
                ProfileSelector(
                    profiles = ui.profiles,
                    onSelect = viewModel::switchProfile,
                    onEdit = { id -> editingProfile = ui.profiles.firstOrNull { it.id == id } },
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${ui.completed} / ${ui.total} servings completed",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    DozenProgressBar(
                        fraction = if (ui.total == 0) 0f else ui.completed.toFloat() / ui.total,
                    )
                }
            }
            items(ui.rows, key = { it.category.id }) { row ->
                CategoryCard(row = row, onToggle = { idx -> viewModel.toggleServing(row.category.id, idx) })
            }
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = { showHistory = true }) { Text("📅 History") }
                    OutlinedButton(onClick = { showResetConfirm = true }) { Text("🔄 Reset Day") }
                    OutlinedButton(onClick = { showCustomize = true }) { Text("⚙️ Customize") }
                    OutlinedButton(onClick = { showAccount = true }) {
                        Text(if (isLoggedIn) "👤 ${ui.email ?: "Account"}" else "👤 Sign In")
                    }
                    OutlinedButton(onClick = { showBlessing = true }) { Text("🙏 Give Thanks") }
                    OutlinedButton(onClick = { shareJson(viewModel.exportFileName(), viewModel.exportJson()) }) {
                        Text("💾 Export")
                    }
                    OutlinedButton(onClick = { pickJson { content -> content?.let(viewModel::importJson) } }) {
                        Text("📂 Import")
                    }
                }
            }
            item {
                Text(
                    text = Devotional.SAINT_MARTHA,
                    style = MaterialTheme.typography.bodySmall,
                    color = DozenPalette.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }

    // ---- Modals ----

    if (showAccount) {
        AccountDialog(
            isLoggedIn = isLoggedIn,
            email = ui.email,
            lastSync = ui.lastSync,
            auth = viewModel.authManager,
            onDismiss = { showAccount = false },
        )
    }

    if (showBlessing) BlessingDialog(onDismiss = { showBlessing = false })

    if (showHistory) {
        val today = DateKeys.today()
        HistoryDialog(
            initialMonth = today.year to today.monthNumber,
            monthProvider = { y, m -> viewModel.historyMonth(y, m) },
            onPickDate = viewModel::navigateToDate,
            onDismiss = { showHistory = false },
        )
    }

    if (showCustomize) {
        CustomizeDialog(
            initialServings = ui.servings,
            onSave = { servings -> viewModel.setCustomServings(servings); showCustomize = false },
            onDismiss = { showCustomize = false },
        )
    }

    celebration?.let { verse ->
        CelebrationDialog(
            profileName = ui.profiles.firstOrNull { it.active }?.name ?: "You",
            verse = verse,
            onDismiss = viewModel::dismissCelebration,
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Day") },
            text = { Text("Are you sure you want to reset all your progress for ${if (ui.isToday) "today" else ui.dateLabel}? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetDay(); showResetConfirm = false
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") } },
        )
    }

    editingProfile?.let { chip ->
        var name by remember(chip.id) { mutableStateOf(chip.name) }
        AlertDialog(
            onDismissRequest = { editingProfile = null },
            title = { Text("Edit Profile") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Profile name") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) viewModel.renameProfile(chip.id, name.trim())
                    editingProfile = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editingProfile = null }) { Text("Cancel") } },
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.UTC).date
                        viewModel.navigateToDate(date)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
