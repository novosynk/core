package com.novosynk.data.di.loader

import com.novosynk.data.di.databaseModule
import com.novosynk.data.di.mediaHandlerModule
import com.novosynk.data.di.repositoryModule
import org.koin.core.context.loadKoinModules

fun loadAllModules() {
    loadKoinModules(
        listOf(
            databaseModule,
            repositoryModule,
        ),
    )
    loadKoinModules(mediaHandlerModule)
    loadMediaService()
}

expect fun loadMediaService()