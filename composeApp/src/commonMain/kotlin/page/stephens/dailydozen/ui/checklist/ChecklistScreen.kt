package page.stephens.dailydozen.ui.checklist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import page.stephens.dailydozen.ui.account.AccountDialog
import page.stephens.dailydozen.ui.account.AccountViewModel
import page.stephens.dailydozen.ui.components.DozenRow
import page.stephens.dailydozen.ui.theme.Harvest

/**
 * The daily checklist, styled to match the web app: a forest-green gradient
 * header over a warm cream page of white category cards. Fully usable signed out.
 */
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

    Column(modifier = modifier.fillMaxSize().background(Harvest.Surface)) {
        // Green gradient header (extends under the status bar).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Harvest.headerGradient)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Daily Dozen",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { showAccount = true }) {
                Text(if (account.signedIn) "Account" else "Sign in", color = Color.White)
            }
        }

        // Contestability banner (a remote un-check was not silently applied).
        if (sync.remoteDeletionsSuppressed) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Harvest.SurfaceContainerLow)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Your devices had differing checks; we kept them all so nothing was lost.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Harvest.OnSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { accountViewModel.acknowledgeRemoteDeletionNotice() }) { Text("OK") }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${state.completedCount} of ${state.totalCount} complete",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Harvest.OnSurface,
                    )
                    ProgressBar(
                        fraction = if (state.totalCount == 0) 0f
                        else state.completedCount.toFloat() / state.totalCount,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    )
                    Text(
                        text = syncLine(account.signedIn, sync.syncing, sync.offline, sync.needsReauth, sync.lastSyncedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = Harvest.OnSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            items(state.progress, key = { it.category.id }) { progress ->
                DozenRow(
                    progress = progress,
                    onSetCount = { count -> viewModel.setCount(progress.category.id, count) },
                )
            }
        }
    }

    if (showAccount) {
        AccountDialog(onDismiss = { showAccount = false }, viewModel = accountViewModel)
    }
}

/** Thick, soft-ended progress bar (no thin clinical line), per the design system. */
@Composable
private fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Harvest.SurfaceContainerHighest),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(6.dp))
                .background(Harvest.headerGradient),
        )
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
