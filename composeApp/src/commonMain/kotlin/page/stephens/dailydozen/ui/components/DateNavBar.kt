package page.stephens.dailydozen.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import page.stephens.dailydozen.ui.theme.DozenPalette

/** Date navigation: ← | tappable date pill | → , with "Return to Today". */
@Composable
fun DateNavBar(
    label: String,
    isToday: Boolean,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPickDate: () -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = onPrev) {
            Text("‹", style = MaterialTheme.typography.headlineSmall)
        }
        Surface(
            onClick = onPickDate,
            shape = RoundedCornerShape(50),
            color = if (isToday) DozenPalette.surfaceContainer else DozenPalette.tertiaryContainer,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = DozenPalette.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        IconButton(onClick = onNext, enabled = canGoNext) {
            Text("›", style = MaterialTheme.typography.headlineSmall, color = if (canGoNext) DozenPalette.onSurface else DozenPalette.outlineVariant)
        }
    }
    if (!isToday) {
        TextButton(onClick = onToday) { Text("Return to Today") }
    }
}
