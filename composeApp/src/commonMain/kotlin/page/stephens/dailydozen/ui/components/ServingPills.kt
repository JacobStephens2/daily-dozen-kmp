package page.stephens.dailydozen.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import page.stephens.dailydozen.ui.theme.DozenPalette

/**
 * The row of tappable serving boxes for one category, numbered 1..[target].
 * A box is filled when its index is in [checked]. Tapping invokes [onToggle];
 * the repository applies the fill-to-left logic, so the UI just reflects state.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServingPills(
    categoryName: String,
    target: Int,
    checked: Set<Int>,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (i in 0 until target) {
            val isChecked = i in checked
            val bg by animateColorAsState(
                if (isChecked) DozenPalette.secondary else DozenPalette.surfaceContainerLow,
            )
            val fg = if (isChecked) DozenPalette.onPrimary else DozenPalette.onSurfaceVariant
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .border(
                        BorderStroke(1.dp, if (isChecked) DozenPalette.secondary else DozenPalette.outlineVariant),
                        RoundedCornerShape(10.dp),
                    )
                    .clickable { onToggle(i) }
                    .semantics {
                        contentDescription = "$categoryName serving ${i + 1} of $target"
                    },
            ) {
                Text(
                    text = "${i + 1}",
                    color = fg,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
            }
        }
    }
}
