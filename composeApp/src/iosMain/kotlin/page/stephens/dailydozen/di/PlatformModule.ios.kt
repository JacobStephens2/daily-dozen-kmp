package page.stephens.dailydozen.di

import org.koin.core.module.Module
import org.koin.dsl.module
import page.stephens.dailydozen.data.DatabaseDriverFactory

actual val platformModule: Module = module {
    single { DatabaseDriverFactory() }
}
