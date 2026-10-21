package com.novosynk.data.di.loader

import com.novosynk.media3.di.loadMediaService
import com.novosynk.data.loginsync.LoginSyncSenderRepositoryImpl
import com.novosynk.data.loginsync.LoginSyncStore
import com.novosynk.domain.repository.LoginSyncSenderRepository
import org.koin.core.context.loadKoinModules
import com.novosynk.loginsync.LoginSyncClient
import org.koin.dsl.module

actual fun loadMediaService() {
    loadMediaService()
}

actual fun loadLoginSyncModule() {
    loadKoinModules(
        module {
            single { LoginSyncStore(get(), get(), get()) }
            single<LoginSyncSenderRepository> { LoginSyncSenderRepositoryImpl(get(), LoginSyncClient()) }
        },
    )
}
