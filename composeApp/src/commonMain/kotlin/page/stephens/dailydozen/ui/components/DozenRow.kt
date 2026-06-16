package page.stephens.dailydozen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import page.stephens.dailydozen.domain.model.CategoryProgress
import page.stephens.dailydozen.ui.theme.Harvest

/**
 * A category card matching the web app: a circular emoji "halo", a linked
 * category name, a serving-size description, and a row of numbered serving chips
 * below. White card body lifted softly off the cream background — no borders.
 */
@Composable
fun DozenRow(
    progress: CategoryProgress,
    onSetCount: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Harvest.CardWhite,
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Harvest.PrimaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = progress.category.emoji,
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = emojiFontFamily(),
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = progress.category.name,
                        color = Harvest.Link,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline,
                    )
                    if (progress.category.description.isNotEmpty()) {
                        Text(
                            text = progress.category.description,
                            color = Harvest.OnSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            ServingChips(count = progress.count, target = progress.target, onSetCount = onSetCount)
        }
    }
}
