package page.stephens.dailydozen.net

import kotlinx.browser.window
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual class NetworkMonitor {
    private val _isOnline = MutableStateFlow(window.navigator.onLine)
    actual val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        window.addEventListener("online") { _isOnline.value = true }
        window.addEventListener("offline") { _isOnline.value = false }
    }
}
