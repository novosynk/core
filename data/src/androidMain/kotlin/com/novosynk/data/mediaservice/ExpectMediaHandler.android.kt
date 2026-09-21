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
    MediaServiceHandlerImpl(
        dataStoreManager,
        songRepository,
        streamRepository,
        localPlaylistRepository,
        analyticsRepository,
        coroutineScope,
    )