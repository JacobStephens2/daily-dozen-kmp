package page.stephens.dailydozen.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * The web app's "Modern Apothecary / harvest hearth" palette, transcribed from
 * `styles.css :root`. Light-only — the original has no dark mode.
 */
object DozenPalette {
    val primary = Color(0xFF38672A)
    val primaryContainer = Color(0xFF508041)
    val onPrimary = Color(0xFFFFFFFF)
    val onPrimaryContainer = Color(0xFFF0F7EE)
    val secondary = Color(0xFF7C5724)
    val secondaryContainer = Color(0xFFBB8F56)
    val tertiary = Color(0xFF71581E)
    val tertiaryContainer = Color(0xFFE1C07B)
    val surface = Color(0xFFFDF9ED)
    val surfaceContainerLow = Color(0xFFF7F3E7)
    val surfaceContainer = Color(0xFFF1EDE1)
    val surfaceContainerHigh = Color(0xFFEBE7DB)
    val surfaceContainerHighest = Color(0xFFE5E1D5)
    val onSurface = Color(0xFF1C1C15)
    val onSurfaceVariant = Color(0xFF46483D)
    val outline = Color(0xFF77796E)
    val outlineVariant = Color(0xFFC7C8B9)
    val error = Color(0xFFBA1A1A)

    // Progress-bar gradient stops (by completion %), matching the web app.
    val gradTan = tertiaryContainer        // #E1C07B
    val gradCopper = secondaryContainer    // #BB8F56
    val gradGreen = primaryContainer       // #508041
    val gradDarkGreen = primary            // #38672a

    // History calendar completion buckets.
    val historyNone = surfaceContainerHighest
    val historyLow = tertiaryContainer
    val historyMid = secondaryContainer
    val historyHigh = primaryContainer
    val historyFull = primary
}

private val LightColors = lightColorScheme(
    primary = DozenPalette.primary,
    onPrimary = DozenPalette.onPrimary,
    primaryContainer = DozenPalette.primaryContainer,
    onPrimaryContainer = DozenPalette.onPrimaryContainer,
    secondary = DozenPalette.secondary,
    onSecondary = Color.White,
    secondaryContainer = DozenPalette.secondaryContainer,
    tertiary = DozenPalette.tertiary,
    tertiaryContainer = DozenPalette.tertiaryContainer,
    background = DozenPalette.surfaceContainerLow,
    onBackground = DozenPalette.onSurface,
    surface = DozenPalette.surface,
    onSurface = DozenPalette.onSurface,
    surfaceVariant = DozenPalette.surfaceContainerHigh,
    onSurfaceVariant = DozenPalette.onSurfaceVariant,
    surfaceContainer = DozenPalette.surfaceContainer,
    surfaceContainerHigh = DozenPalette.surfaceContainerHigh,
    surfaceContainerHighest = DozenPalette.surfaceContainerHighest,
    outline = DozenPalette.outline,
    outlineVariant = DozenPalette.outlineVariant,
    error = DozenPalette.error,
)

@Composable
fun DailyDozenTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}

/** Parse a "#rrggbb" (or "#aarrggbb") hex string into a Compose [Color]. */
fun parseHexColor(hex: String, fallback: Color = DozenPalette.primary): Color {
    val cleaned = hex.removePrefix("#")
    val value = cleaned.toLongOrNull(16) ?: return fallback
    return when (cleaned.length) {
        6 -> Color(0xFF000000 or value)
        8 -> Color(value)
        else -> fallback
    }
}
