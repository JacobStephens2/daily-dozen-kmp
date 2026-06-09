package page.stephens.dailydozen.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/**
 * The font family to use for emoji glyphs, or null to use the platform default.
 *
 * Android returns null: the OS supplies color emoji on every supported API level,
 * and forcing our bundled COLRv1 font would actually REGRESS color on Android < 13
 * (platform COLRv1 support starts at API 33). The Skia-based targets (iOS, web)
 * have no system emoji font, so they return the bundled Noto Color Emoji (COLRv1,
 * which Skia renders in color; the CBDT bitmap variant does not render in Skiko).
 */
@Composable
expect fun emojiFontFamily(): FontFamily?
