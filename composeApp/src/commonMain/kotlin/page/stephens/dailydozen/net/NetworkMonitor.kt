package page.stephens.dailydozen.net

import kotlinx.coroutines.flow.StateFlow

/** Reports connectivity so the UI can show the "you are offline" banner. */
expect class NetworkMonitor {
    val isOnline: StateFlow<Boolean>
}
