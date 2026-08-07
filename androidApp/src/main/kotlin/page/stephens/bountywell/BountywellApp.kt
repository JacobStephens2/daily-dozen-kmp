package page.stephens.bountywell

import android.app.Application
import org.koin.android.ext.koin.androidContext
import page.stephens.bountywell.di.initKoin

/**
 * Starts Koin once for the process, handing it the application Context the
 * Android SQLDelight driver needs. All other wiring lives in the shared modules.
 */
class BountywellApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin { androidContext(this@BountywellApp) }
    }
}
