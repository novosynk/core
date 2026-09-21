package com.novosynk.data.mediaservice

actual fun createMediaServiceHandler(
    dataStoreManager: com.novosynk.domain.manager.DataStoreManager,
    songRepository: com.novosynk.domain.repository.SongRepository,
    streamRepository: com.novosynk.domain.repository.StreamRepository,
    localPlaylistRepository: com.novosynk.domain.repository.LocalPlaylistRepository,
    analyticsRepository: com.novosynk.domain.repository.AnalyticsRepository,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
): com.novosynk.domain.mediaservice.handler.MediaPlayerHandler {
    TODO("Not yet implemented")
}