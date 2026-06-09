package page.stephens.dailydozen.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

// Android supplies color emoji via the OS; don't override. Forcing the bundled
// COLRv1 font would lose color on Android < 13 (platform COLRv1 starts at API 33).
@Composable
actual fun emojiFontFamily(): FontFamily? = null
