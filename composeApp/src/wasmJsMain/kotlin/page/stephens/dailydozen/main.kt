package page.stephens.dailydozen

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

/**
 * Web (Wasm) launcher. Boots the shared [App] onto a full-window canvas.
 * No business or UI logic lives here.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        App()
    }
}
