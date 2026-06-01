package page.stephens.dailydozen.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.unit.dp
import page.stephens.dailydozen.ui.CategoryRow
import page.stephens.dailydozen.ui.theme.DozenPalette

/**
 * A single category: emoji + a name that links to NutritionFacts.org, the
 * serving-size hint, and the tappable serving boxes. Tints when complete.
 */
@Composable
fun CategoryCard(
    row: CategoryRow,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cat = row.category
    val complete = row.checked.size >= row.servings
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (complete) DozenPalette.primaryContainer else DozenPalette.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = cat.icon,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    CategoryName(cat)
                    Text(
                        text = cat.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = DozenPalette.onSurfaceVariant,
                    )
                }
                Text(
                    text = "${row.checked.size}/${row.servings}",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (complete) DozenPalette.primary else DozenPalette.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
            }
            ServingPills(
                categoryName = cat.name,
                target = row.servings,
                checked = row.checked,
                onToggle = onToggle,
            )
        }
    }
}

@Composable
private fun CategoryName(cat: page.stephens.dailydozen.domain.model.DozenCategory) {
    val linkStyle = TextLinkStyles(
        style = SpanStyle(color = DozenPalette.primary, fontWeight = FontWeight.SemiBold),
    )
    val text = buildAnnotatedString {
        val links = cat.links
        when {
            links.isEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(cat.name) }
            links.size == 1 -> withLink(LinkAnnotation.Url(links[0].url, linkStyle)) { append(cat.name) }
            else -> {
                // e.g. "Nuts and Seeds" rendered as two labelled links.
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("") }
                links.forEachIndexed { i, link ->
                    if (i > 0) {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(" and ") }
                    }
                    withLink(LinkAnnotation.Url(link.url, linkStyle)) { append(link.label ?: cat.name) }
                }
            }
        }
    }
    Text(text = text, style = MaterialTheme.typography.titleMedium)
}
