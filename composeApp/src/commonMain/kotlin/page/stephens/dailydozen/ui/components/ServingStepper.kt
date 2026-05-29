package page.stephens.dailydozen.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * A tappable serving stepper: minus / current count / plus.
 */
@Composable
fun ServingStepper(
    count: Int,
    target: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = onDecrement, enabled = count > 0) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = "$count / $target",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.size(width = 52.dp, height = 24.dp),
        )
        FilledIconButton(onClick = onIncrement, shape = CircleShape) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }
}
