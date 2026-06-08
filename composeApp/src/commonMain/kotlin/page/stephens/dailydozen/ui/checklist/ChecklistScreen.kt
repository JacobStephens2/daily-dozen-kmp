package page.stephens.dailydozen.ui.checklist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import page.stephens.dailydozen.ui.account.AccountDialog
import page.stephens.dailydozen.ui.account.AccountViewModel
import page.stephens.dailydozen.ui.components.DozenRow

/**
 * The daily checklist: the active Daily Dozen categories, each with a tappable
 * serving stepper, a completion header, a sync-status line, and an Account
 * entry point. Fully usable signed out (local-first).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(
    modifier: Modifier = Modifier,
    viewModel: ChecklistViewModel = koinViewModel(),
    accountViewModel: AccountViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val account by accountViewModel.state.collectAsStateWithLifecycle()
    val sync by accountViewModel.syncStatus.collectAsStateWithLifecycle()
    var showAccount by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Daily Dozen") },
                actions = {
                    TextButton(onClick = { showAccount = true }) {
                        Text(if (account.signedIn) "Account" else "Sign in")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Contestability banner (GPT-5.5): a remote un-check was not silently applied.
            if (sync.remoteDeletionsSuppressed) {
                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Your devices had differing checks; we kept them all so nothing was lost.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { accountViewModel.acknowledgeRemoteDeletionNotice() }) { Text("OK") }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text(
                            text = "${state.completedCount} of ${state.totalCount} complete",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        LinearProgressIndicator(
                            progress = {
                                if (state.totalCount == 0) 0f
                                else state.completedCount.toFloat() / state.totalCount
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                        Text(
                            text = syncLine(account.signedIn, sync.syncing, sync.offline, sync.needsReauth, sync.lastSyncedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                items(state.progress, key = { it.category.id }) { progress ->
                    DozenRow(
                        progress = progress,
                        onIncrement = { viewModel.increment(progress.category.id) },
                        onDecrement = { viewModel.decrement(progress.category.id) },
                    )
                }
            }
        }
    }

    if (showAccount) {
        AccountDialog(onDismiss = { showAccount = false }, viewModel = accountViewModel)
    }
}

private fun syncLine(
    signedIn: Boolean,
    syncing: Boolean,
    offline: Boolean,
    needsReauth: Boolean,
    lastSyncedAt: String?,
): String = when {
    !signedIn -> "Saved on this device · sign in to sync"
    needsReauth -> "Session expired · sign in again (data is safe)"
    syncing -> "Syncing…"
    offline -> "Offline · changes saved locally, will sync later"
    lastSyncedAt != null -> "Synced"
    else -> "Signed in"
}
