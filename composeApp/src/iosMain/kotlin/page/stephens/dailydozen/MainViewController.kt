package page.stephens.dailydozen

import androidx.compose.ui.window.ComposeUIViewController
import page.stephens.dailydozen.di.initKoin

private var koinStarted = false

/**
 * iOS entry point. Returns a UIViewController hosting the shared [App], which
 * the SwiftUI launcher embeds. Starts Koin once on first creation (the iOS
 * launcher previously lacked an initKoin() call), so the shared graph — incl.
 * the Keychain TokenStore and sync stack — is wired before [App] composes.
 */
fun MainViewController() = ComposeUIViewController {
    if (!koinStarted) {
        initKoin()
        koinStarted = true
    }
    App()
}
