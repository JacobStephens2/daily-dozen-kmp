package page.stephens.dailydozen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import page.stephens.dailydozen.ui.theme.Harvest

/**
 * Numbered serving chips, matching the web app: one tonal square per target
 * serving. Filled (logged) chips use the warm "earthy check" colour; tapping a
 * chip sets the count up to it, or un-sets it if it's already the last filled one.
 */
@Composable
fun ServingChips(
    count: Int,
    target: Int,
    onSetCount: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 1..target) {
            val filled = i <= count
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (filled) Harvest.Secondary else Harvest.SurfaceContainerHigh)
                    .clickable { onSetCount(if (i == count) i - 1 else i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = i.toString(),
                    color = if (filled) Color.White else Harvest.OnSurfaceVariant,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
