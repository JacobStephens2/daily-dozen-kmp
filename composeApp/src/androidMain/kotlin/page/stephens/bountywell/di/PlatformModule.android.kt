package page.stephens.bountywell.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import page.stephens.bountywell.data.DatabaseDriverFactory
import page.stephens.bountywell.data.auth.TokenStore

actual val platformModule: Module = module {
    single { DatabaseDriverFactory(androidContext()) }
    single { TokenStore(androidContext()) }
}
