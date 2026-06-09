package page.stephens.dailydozen.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.compose.resources.Font
import page.stephens.dailydozen.resources.Res
import page.stephens.dailydozen.resources.noto_color_emoji

// Skia (iOS) has no system emoji font; use the bundled COLRv1 Noto Color Emoji.
@Composable
actual fun emojiFontFamily(): FontFamily? = FontFamily(Font(Res.font.noto_color_emoji))
