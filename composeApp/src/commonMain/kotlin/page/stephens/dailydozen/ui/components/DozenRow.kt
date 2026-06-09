package page.stephens.dailydozen.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import page.stephens.dailydozen.domain.model.CategoryProgress

/**
 * A single Daily Dozen category row: emoji + name on the left, a serving
 * stepper on the right. The card tints green once the target is met.
 */
@Composable
fun DozenRow(
    progress: CategoryProgress,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container by animateColorAsState(
        if (progress.isComplete) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
    )
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Emoji glyphs: on Android the OS supplies them; on Skia targets
            // (iOS/web) we supply a bundled color font (see emojiFontFamily()).
            Text(
                text = progress.category.emoji,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = emojiFontFamily(),
                modifier = Modifier.padding(end = 12.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = progress.category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (progress.isComplete) "Complete" else "${progress.remaining} to go",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ServingStepper(
                count = progress.count,
                target = progress.target,
                onIncrement = onIncrement,
                onDecrement = onDecrement,
            )
        }
    }
}
