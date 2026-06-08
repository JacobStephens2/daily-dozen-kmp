package page.stephens.dailydozen.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

/**
 * Account sheet: signed-out shows the auth form (login/register/forgot);
 * signed-in shows sync status and the data-dignity actions (export, local-only
 * delete, logout — none of which silently destroy local data).
 */
@Composable
fun AccountDialog(
    onDismiss: () -> Unit,
    viewModel: AccountViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sync by viewModel.syncStatus.collectAsStateWithLifecycle()

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = androidx.compose.material3.MaterialTheme.shapes.large, tonalElevation = 6.dp) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when {
                    state.exportJson != null -> ExportView(state.exportJson!!, viewModel::dismissExport)
                    state.signedIn -> SignedInView(state, sync.lastSyncedAt, sync.needsReauth, viewModel, onDismiss)
                    else -> AuthForm(state, viewModel)
                }

                state.message?.let { Text(it, style = androidx.compose.material3.MaterialTheme.typography.bodySmall) }
                if (state.loading) CircularProgressIndicator()

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            }
        }
    }
}

@Composable
private fun AuthForm(state: AccountUiState, viewModel: AccountViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val title = when (state.mode) {
        AuthMode.LOGIN -> "Sign in to sync"
        AuthMode.REGISTER -> "Create an account"
        AuthMode.FORGOT -> "Reset your password"
    }
    Text(title, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
    Text(
        "Your data is saved on this device. Signing in syncs it across your devices — it's optional.",
        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
    )

    OutlinedTextField(
        value = email, onValueChange = { email = it },
        label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
    )
    if (state.mode != AuthMode.FORGOT) {
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password (min 8)") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
    }

    Button(
        onClick = {
            when (state.mode) {
                AuthMode.LOGIN -> viewModel.login(email, password)
                AuthMode.REGISTER -> viewModel.register(email, password)
                AuthMode.FORGOT -> viewModel.forgotPassword(email)
            }
        },
        enabled = !state.loading,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            when (state.mode) {
                AuthMode.LOGIN -> "Sign in"
                AuthMode.REGISTER -> "Create account"
                AuthMode.FORGOT -> "Send reset link"
            },
        )
    }

    when (state.mode) {
        AuthMode.LOGIN -> {
            TextButton(onClick = { viewModel.setMode(AuthMode.REGISTER) }) { Text("New here? Create an account") }
            TextButton(onClick = { viewModel.setMode(AuthMode.FORGOT) }) { Text("Forgot password?") }
        }
        else -> TextButton(onClick = { viewModel.setMode(AuthMode.LOGIN) }) { Text("Back to sign in") }
    }
}

@Composable
private fun SignedInView(
    state: AccountUiState,
    lastSyncedAt: String?,
    needsReauth: Boolean,
    viewModel: AccountViewModel,
    onDismiss: () -> Unit,
) {
    var confirmingDelete by remember { mutableStateOf(false) }

    Text("Account", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
    state.email?.let { Text("Signed in as $it") }
    Text(
        if (needsReauth) "Session expired — sign in again to resume syncing. Your data is safe on this device."
        else "Last synced: ${lastSyncedAt ?: "not yet"}",
        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
    )

    HorizontalDivider()

    OutlinedButton(onClick = { viewModel.exportData() }, modifier = Modifier.fillMaxWidth()) {
        Text("Export my data")
    }
    OutlinedButton(onClick = { viewModel.logout() }, modifier = Modifier.fillMaxWidth()) {
        Text("Sign out (keeps data on this device)")
    }

    HorizontalDivider()

    if (!confirmingDelete) {
        TextButton(onClick = { confirmingDelete = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Delete data on this device…")
        }
    } else {
        Text(
            "This deletes your logged days on THIS device only. There is no server-side delete, " +
                "so data already synced to your account stays on the server and your other devices.",
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        )
        Button(
            onClick = { viewModel.deleteLocalData(); confirmingDelete = false },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Delete local data") }
        TextButton(onClick = { confirmingDelete = false }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}

@Composable
private fun ExportView(json: String, onDone: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    Text("Export", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
    Text("Your full data as JSON. Copy it somewhere safe.", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
    Button(onClick = { clipboard.setText(AnnotatedString(json)) }, modifier = Modifier.fillMaxWidth()) {
        Text("Copy to clipboard")
    }
    SelectionContainer {
        Text(json, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
    }
    TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
}
