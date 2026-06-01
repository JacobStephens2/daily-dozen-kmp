package page.stephens.dailydozen.ui.modals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import page.stephens.dailydozen.net.AuthManager
import page.stephens.dailydozen.ui.theme.DozenPalette

private enum class AuthMode { SignIn, Register, Forgot }

/** Sign in / register / forgot-password, and the signed-in account panel. */
@Composable
fun AccountDialog(
    isLoggedIn: Boolean,
    email: String?,
    lastSync: String?,
    auth: AuthManager,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(AuthMode.SignIn) }
    var emailField by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = DozenPalette.surface) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (isLoggedIn) {
                    Text("Account", style = MaterialTheme.typography.headlineSmall, color = DozenPalette.primary)
                    Text(email ?: "", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Last synced: ${lastSync ?: "Never"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DozenPalette.onSurfaceVariant,
                    )
                    Button(
                        onClick = {
                            busy = true; status = ""; error = false
                            scope.launch {
                                runCatching { auth.syncNow() }
                                    .onSuccess { status = "Synced successfully" }
                                    .onFailure { status = it.message ?: "Sync failed"; error = true }
                                busy = false
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (busy) "Syncing…" else "Sync Now") }
                    OutlinedButton(
                        onClick = { scope.launch { auth.logout(); onDismiss() } },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Sign Out") }
                } else {
                    val title = when (mode) {
                        AuthMode.SignIn -> "Sign In"
                        AuthMode.Register -> "Create Account"
                        AuthMode.Forgot -> "Reset Password"
                    }
                    Text(title, style = MaterialTheme.typography.headlineSmall, color = DozenPalette.primary)

                    OutlinedTextField(
                        value = emailField,
                        onValueChange = { emailField = it },
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (mode != AuthMode.Forgot) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password (8+ characters)") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Button(
                        onClick = {
                            busy = true; status = ""; error = false
                            scope.launch {
                                runCatching {
                                    when (mode) {
                                        AuthMode.SignIn -> { auth.login(emailField.trim(), password); null }
                                        AuthMode.Register -> { auth.register(emailField.trim(), password); null }
                                        AuthMode.Forgot -> auth.forgotPassword(emailField.trim())
                                    }
                                }.onSuccess { msg ->
                                    if (mode == AuthMode.Forgot) {
                                        status = msg ?: "Reset link sent"; busy = false
                                    } else onDismiss()
                                }.onFailure {
                                    status = it.message ?: "Request failed"; error = true; busy = false
                                }
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            when (mode) {
                                AuthMode.SignIn -> if (busy) "Signing in…" else "Sign In"
                                AuthMode.Register -> if (busy) "Creating…" else "Create Account"
                                AuthMode.Forgot -> if (busy) "Sending…" else "Send Reset Link"
                            },
                        )
                    }

                    when (mode) {
                        AuthMode.SignIn -> {
                            TextButton(onClick = { mode = AuthMode.Register; status = "" }) {
                                Text("Don't have an account? Create one")
                            }
                            TextButton(onClick = { mode = AuthMode.Forgot; status = "" }) {
                                Text("Forgot password?")
                            }
                        }
                        AuthMode.Register -> TextButton(onClick = { mode = AuthMode.SignIn; status = "" }) {
                            Text("Already have an account? Sign in")
                        }
                        AuthMode.Forgot -> TextButton(onClick = { mode = AuthMode.SignIn; status = "" }) {
                            Text("Back to Sign In")
                        }
                    }
                }

                if (status.isNotEmpty()) {
                    Text(
                        status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (error) DozenPalette.error else DozenPalette.primary,
                    )
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            }
        }
    }
}
