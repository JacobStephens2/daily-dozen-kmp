package page.stephens.dailydozen.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * The "Harvest Hearth / Modern Apothecary" palette, matching the web app's
 * design system (see the PWA's DESIGN.md): a warm cream base, deep-forest green,
 * and earthy accents — no harsh borders or pure white backgrounds.
 *
 * The web app has no dark mode, so this theme is always the cream/light scheme,
 * regardless of the device setting, so the app looks like the website everywhere.
 */
object Harvest {
    val Primary = Color(0xFF38672A) // deep forest green
    val PrimaryContainer = Color(0xFF508041) // leafy green (icon halos, gradient end)
    val Secondary = Color(0xFF7C5724) // warm earth brown (filled serving chips)
    val Tertiary = Color(0xFF71581E)

    val Surface = Color(0xFFFDF9ED) // warm cream base (never pure white)
    val SurfaceContainerLow = Color(0xFFF7F3E7)
    val SurfaceContainerHigh = Color(0xFFEBE7DB) // unfilled serving chips
    val SurfaceContainerHighest = Color(0xFFE5E1D5) // progress track
    val CardWhite = Color(0xFFFFFFFF) // interactive cards "pop" off the cream

    val OnSurface = Color(0xFF1C1C15)
    val OnSurfaceVariant = Color(0xFF46483D) // descriptions / metadata
    val OutlineVariant = Color(0xFFC7C8B9)

    val Link = Color(0xFF1A6CC9) // category-name links (matches the web app)

    /** 135° primary gradient used for the header and primary CTAs. */
    val headerGradient = Brush.linearGradient(listOf(Primary, PrimaryContainer))
}

private val HarvestColors = lightColorScheme(
    primary = Harvest.Primary,
    onPrimary = Color.White,
    primaryContainer = Harvest.PrimaryContainer,
    onPrimaryContainer = Color(0xFFF0F7EE),
    secondary = Harvest.Secondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBB8F56),
    tertiary = Harvest.Tertiary,
    tertiaryContainer = Color(0xFFE1C07B),
    background = Harvest.Surface,
    onBackground = Harvest.OnSurface,
    surface = Harvest.Surface,
    onSurface = Harvest.OnSurface,
    surfaceVariant = Harvest.SurfaceContainerHigh,
    onSurfaceVariant = Harvest.OnSurfaceVariant,
    outlineVariant = Harvest.OutlineVariant,
)

@Composable
fun DailyDozenTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = HarvestColors,
        content = content,
    )
}
