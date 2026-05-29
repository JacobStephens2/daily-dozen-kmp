package page.stephens.dailydozen

import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview
import page.stephens.dailydozen.ui.checklist.ChecklistScreen
import page.stephens.dailydozen.ui.theme.DailyDozenTheme

/**
 * Root composable shared by every platform launcher (Android, iOS, Wasm).
 * Launchers contain no UI of their own — they only call [App].
 */
@Composable
@Preview
fun App() {
    DailyDozenTheme {
        ChecklistScreen()
    }
}
