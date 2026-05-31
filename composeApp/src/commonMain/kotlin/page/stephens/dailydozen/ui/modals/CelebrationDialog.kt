package page.stephens.dailydozen.ui.modals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import page.stephens.dailydozen.domain.Devotional
import page.stephens.dailydozen.ui.theme.DozenPalette

/** Shown once per day when all servings are complete (matches the web app). */
@Composable
fun CelebrationDialog(
    profileName: String,
    verse: Devotional.Verse,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = DozenPalette.surface) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("🎉", style = MaterialTheme.typography.displaySmall)
                Text(
                    "Congratulations!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = DozenPalette.primary,
                )
                Text(
                    "$profileName has completed the Daily Dozen for today!",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    Devotional.CELEBRATION_MESSAGE,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DozenPalette.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "“${verse.text}”",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "— ${verse.reference}",
                    style = MaterialTheme.typography.labelMedium,
                    color = DozenPalette.secondary,
                )
                Button(onClick = onDismiss) { Text("Amen! 🙏") }
            }
        }
    }
}
