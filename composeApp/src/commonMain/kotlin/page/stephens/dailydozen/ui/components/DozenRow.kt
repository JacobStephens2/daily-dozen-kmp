package page.stephens.dailydozen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import page.stephens.dailydozen.domain.model.CategoryProgress
import page.stephens.dailydozen.ui.theme.Harvest

/**
 * A category card. Collapsed it's a single row — emoji halo, the (linked) name,
 * and the serving chips. Tapping the name expands the card to reveal the sample
 * serving sizes and "more info" links (NutritionFacts.org).
 */
@Composable
fun DozenRow(
    progress: CategoryProgress,
    onSetCount: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(progress.category.id) { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Harvest.CardWhite,
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Harvest.PrimaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = progress.category.emoji,
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = emojiFontFamily(),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = progress.category.name,
                    color = Harvest.Link,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { expanded = !expanded }
                        .padding(vertical = 6.dp),
                )
                Spacer(Modifier.width(8.dp))
                ServingChips(count = progress.count, target = progress.target, onSetCount = onSetCount)
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                if (progress.category.description.isNotEmpty()) {
                    Text(
                        text = progress.category.description,
                        color = Harvest.OnSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (progress.category.infoLinks.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        progress.category.infoLinks.forEach { link ->
                            Text(
                                text = "${link.label} ↗",
                                color = Harvest.Link,
                                style = MaterialTheme.typography.bodySmall,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable { uriHandler.openUri(link.url) },
                            )
                        }
                    }
                }
            }
        }
    }
}
