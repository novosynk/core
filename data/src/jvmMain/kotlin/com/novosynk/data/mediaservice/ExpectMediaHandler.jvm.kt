package com.novosynk.data.mediaservice

import com.novosynk.domain.repository.AnalyticsRepository

actual fun createMediaServiceHandler(
    dataStoreManager: com.novosynk.domain.manager.DataStoreManager,
    songRepository: com.novosynk.domain.repository.SongRepository,
    streamRepository: com.novosynk.domain.repository.StreamRepository,
    localPlaylistRepository: com.novosynk.domain.repository.LocalPlaylistRepository,
    analyticsRepository: AnalyticsRepository,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
): com.novosynk.domain.mediaservice.handler.MediaPlayerHandler =
    JvmMediaPlayerHandlerImpl(
        dataStoreManager = dataStoreManager,
        songRepository = songRepository,
        streamRepository = streamRepository,
        localPlaylistRepository = localPlaylistRepository,
        analyticsRepository = analyticsRepository,
        coroutineScope = coroutineScope,
    )