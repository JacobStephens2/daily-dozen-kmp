package page.stephens.bountywell

import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview
import page.stephens.bountywell.ui.checklist.ChecklistScreen
import page.stephens.bountywell.ui.theme.BountywellTheme

/**
 * Root composable shared by every platform launcher (Android, iOS, Wasm).
 * Launchers contain no UI of their own — they only call [App].
 */
@Composable
@Preview
fun App() {
    BountywellTheme {
        ChecklistScreen()
    }
}
