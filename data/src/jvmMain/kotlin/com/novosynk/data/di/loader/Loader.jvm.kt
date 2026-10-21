package com.novosynk.data.di.loader

import com.simpmusic.media_jvm.di.loadDesktopPlayerModule
import com.novosynk.data.loginsync.LoginSyncHostRepositoryImpl
import com.novosynk.data.loginsync.LoginSyncStore
import com.novosynk.domain.repository.LoginSyncHostRepository
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

actual fun loadMediaService() {
    loadDesktopPlayerModule()
}

actual fun loadLoginSyncModule() {
    loadKoinModules(
        module {
            single { LoginSyncStore(get(), get(), get()) }
            single<LoginSyncHostRepository> { LoginSyncHostRepositoryImpl(get()) }
        },
    )
}
