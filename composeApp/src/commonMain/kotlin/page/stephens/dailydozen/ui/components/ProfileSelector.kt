package page.stephens.dailydozen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import page.stephens.dailydozen.ui.ProfileChip
import page.stephens.dailydozen.ui.theme.DozenPalette
import page.stephens.dailydozen.ui.theme.parseHexColor

/** Horizontal row of profile chips; the active one shows an ✏️ edit affordance. */
@Composable
fun ProfileSelector(
    profiles: List<ProfileChip>,
    onSelect: (String) -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        profiles.forEach { p ->
            val color = parseHexColor(p.color)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (p.active) color else DozenPalette.surfaceContainerHigh)
                    .clickable { onSelect(p.id) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "👤 ${p.name}",
                    color = if (p.active) Color.White else DozenPalette.onSurfaceVariant,
                    fontWeight = if (p.active) FontWeight.SemiBold else FontWeight.Normal,
                )
                if (p.active) {
                    Text(
                        text = "✏️",
                        modifier = Modifier.clickable { onEdit(p.id) }.padding(start = 2.dp),
                    )
                }
            }
        }
    }
}
