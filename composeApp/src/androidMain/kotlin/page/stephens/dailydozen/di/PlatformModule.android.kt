package page.stephens.dailydozen.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import page.stephens.dailydozen.data.DatabaseDriverFactory
import page.stephens.dailydozen.net.NetworkMonitor
import page.stephens.dailydozen.net.SecureTokenStore

actual val platformModule: Module = module {
    single { DatabaseDriverFactory(androidContext()) }
    single { SecureTokenStore(androidContext()) }
    single { NetworkMonitor(androidContext()) }
}
