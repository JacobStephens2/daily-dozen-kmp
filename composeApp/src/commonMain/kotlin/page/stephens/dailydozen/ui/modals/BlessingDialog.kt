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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import page.stephens.dailydozen.domain.Devotional
import page.stephens.dailydozen.ui.theme.DozenPalette

/** The "Give Thanks" 🙏 Blessing Before Meals modal. */
@Composable
fun BlessingDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = DozenPalette.surface) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "Blessing Before Meals",
                    style = MaterialTheme.typography.headlineSmall,
                    color = DozenPalette.primary,
                )
                Devotional.blessingLines.forEach { line ->
                    Text(
                        line,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                }
                Button(onClick = onDismiss) { Text("Amen") }
            }
        }
    }
}
