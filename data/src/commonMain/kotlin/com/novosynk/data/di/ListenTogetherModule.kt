package com.novosynk.data.di

import com.novosynk.common.AppIdentity
import com.novosynk.common.Config
import com.novosynk.data.listentogether.ListenTogetherPlaybackBridge
import com.novosynk.data.listentogether.ListenTogetherPrefs
import com.novosynk.data.listentogether.ListenTogetherRepositoryImpl
import com.novosynk.domain.repository.ListenTogetherRepository
import com.novosynk.domain.manager.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.qualifier.named
import org.koin.dsl.module
import com.novosynk.listentogether.ListenTogetherClient
import com.novosynk.listentogether.ListenTogetherSession

/**
 * Listen Together lives for as long as the app does, not as long as its screen.
 *
 * The session was originally created inside the ViewModel, which meant leaving the screen tore the
 * socket down and left the room — while playback, the thing the room is about, kept running in the
 * background. Singletons here, and the screen merely observes them.
 */
val listenTogetherModule =
    module {
        single {
            ListenTogetherClient(
                clientVersion = "Musico",
                userAgent = get<AppIdentity>().userAgent,
                // Read per connection attempt, so a server changed in settings applies next time.
                serverUrl = {
                    runBlocking { get<DataStoreManager>().getString(ListenTogetherPrefs.SERVER_URL).first().orEmpty() }
                },
            )
        }

        single { ListenTogetherSession(client = get()) }

        // The boundary: everything above this line speaks domain types only.
        single<ListenTogetherRepository> {
            ListenTogetherRepositoryImpl(
                session = get(),
                scope = get<CoroutineScope>(named(Config.SERVICE_SCOPE)),
            )
        }

        // createdAtStart: the bridge has to be listening before the user joins anything, otherwise
        // the first track change of a room is missed and the guest sits in silence.
        single(createdAtStart = true) {
            ListenTogetherPlaybackBridge(
                repository = get(),
                session = get(),
                handler = get(),
                scope = get<CoroutineScope>(named(Config.SERVICE_SCOPE)),
            ).also { it.start() }
        }
    }
