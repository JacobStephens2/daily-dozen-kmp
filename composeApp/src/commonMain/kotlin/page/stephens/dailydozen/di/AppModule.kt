package page.stephens.dailydozen.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import page.stephens.dailydozen.data.DozenRepository
import page.stephens.dailydozen.data.remote.SyncApi
import page.stephens.dailydozen.data.remote.createSyncHttpClient
import page.stephens.dailydozen.data.sync.SyncEngine
import page.stephens.dailydozen.ui.account.AccountViewModel
import page.stephens.dailydozen.ui.checklist.ChecklistViewModel

/** Cross-platform wiring: the repository, the sync stack, and the ViewModels. */
val appModule: Module = module {
    single { DozenRepository(get()) }
    single { createSyncHttpClient(get()) } // TokenStore from platformModule
    single { SyncApi(get(), get()) } // HttpClient, TokenStore
    single { SyncEngine(get(), get(), get()) } // SyncApi, DozenRepository, TokenStore
    viewModelOf(::ChecklistViewModel)
    viewModelOf(::AccountViewModel)
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
