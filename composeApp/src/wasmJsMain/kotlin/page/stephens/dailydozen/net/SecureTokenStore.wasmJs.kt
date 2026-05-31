package page.stephens.dailydozen.net

import kotlinx.browser.localStorage
import org.w3c.dom.get
import org.w3c.dom.set

actual class SecureTokenStore {
    actual fun getToken(): String? = localStorage["dailyDozenAuthToken"]

    actual fun setToken(token: String?) {
        if (token == null) localStorage.removeItem("dailyDozenAuthToken")
        else localStorage["dailyDozenAuthToken"] = token
    }
}
