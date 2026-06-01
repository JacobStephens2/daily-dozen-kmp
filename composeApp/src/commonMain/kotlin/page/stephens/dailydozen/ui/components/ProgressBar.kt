package page.stephens.dailydozen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import page.stephens.dailydozen.ui.theme.DozenPalette

/**
 * Thick rounded progress bar whose fill gradient shifts with completion,
 * matching the web app: tan→copper (<50%), copper→green (50–75%), green→dark
 * green (≥75%).
 */
@Composable
fun DozenProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f))
    val brush = when {
        animated < 0.5f -> Brush.horizontalGradient(listOf(DozenPalette.gradTan, DozenPalette.gradCopper))
        animated < 0.75f -> Brush.horizontalGradient(listOf(DozenPalette.gradCopper, DozenPalette.gradGreen))
        else -> Brush.horizontalGradient(listOf(DozenPalette.gradGreen, DozenPalette.gradDarkGreen))
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(50))
            .background(DozenPalette.surfaceContainerHighest),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .height(12.dp)
                .clip(RoundedCornerShape(50))
                .background(brush),
        )
    }
}
