package page.stephens.dailydozen.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import page.stephens.dailydozen.data.DozenRepository
import page.stephens.dailydozen.net.ApiClient
import page.stephens.dailydozen.net.AuthManager
import page.stephens.dailydozen.ui.DailyDozenViewModel

/** Cross-platform wiring: the repository, sync layer, and the ViewModel. */
val appModule: Module = module {
    single { DozenRepository(get()) }
    single { ApiClient() }
    single { AuthManager(get(), get(), get()) }
    viewModelOf(::DailyDozenViewModel)
}

/**
 * Platform-specific wiring — chiefly the [page.stephens.dailydozen.data.DatabaseDriverFactory],
 * whose construction differs per target (Android needs a Context).
 */
expect val platformModule: Module

/** Single entry point each launcher calls once at startup. */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(appModule, platformModule)
}
