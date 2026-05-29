package page.stephens.dailydozen

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import page.stephens.dailydozen.di.initKoin

/**
 * Web (Wasm) launcher. Starts Koin, then boots the shared [App] onto a
 * full-window canvas. No business or UI logic lives here.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin()
    ComposeViewport(document.body!!) {
        App()
    }
}
