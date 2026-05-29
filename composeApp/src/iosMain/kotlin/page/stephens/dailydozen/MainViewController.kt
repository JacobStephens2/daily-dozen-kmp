package page.stephens.dailydozen

import androidx.compose.ui.window.ComposeUIViewController

/**
 * iOS entry point. Returns a UIViewController hosting the shared [App],
 * which the SwiftUI launcher embeds. No logic lives here.
 */
fun MainViewController() = ComposeUIViewController { App() }
