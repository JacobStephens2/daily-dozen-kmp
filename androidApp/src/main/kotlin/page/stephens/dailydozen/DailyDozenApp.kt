package page.stephens.dailydozen

import android.app.Application
import org.koin.android.ext.koin.androidContext
import page.stephens.dailydozen.di.initKoin
import page.stephens.dailydozen.platform.AndroidAppContext

/**
 * Starts Koin once for the process, handing it the application Context the
 * Android SQLDelight driver needs. All other wiring lives in the shared modules.
 */
class DailyDozenApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidAppContext.context = this
        initKoin { androidContext(this@DailyDozenApp) }
    }
}
