package page.stephens.dailydozen.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import page.stephens.dailydozen.data.DozenRepository
import page.stephens.dailydozen.ui.checklist.ChecklistViewModel

/** Cross-platform wiring: the repository and the ViewModels. */
val appModule: Module = module {
    single { DozenRepository(get()) }
    viewModelOf(::ChecklistViewModel)
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
