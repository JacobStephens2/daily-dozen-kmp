package page.stephens.dailydozen.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// A warm, garden-leaning Material 3 palette suited to a nutrition tracker.
private val Green = Color(0xFF3E6B41)
private val GreenContainer = Color(0xFFB9F0B5)
private val Wheat = Color(0xFF7A5900)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = GreenContainer,
    onPrimaryContainer = Color(0xFF00210A),
    secondary = Wheat,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DD49A),
    onPrimary = Color(0xFF0B3911),
    primaryContainer = Color(0xFF26512B),
    onPrimaryContainer = GreenContainer,
)

@Composable
fun DailyDozenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
