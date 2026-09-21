package com.novosynk.data.mediaservice

import com.novosynk.domain.manager.DataStoreManager
import com.novosynk.domain.mediaservice.handler.MediaPlayerHandler
import com.novosynk.domain.repository.AnalyticsRepository
import com.novosynk.domain.repository.LocalPlaylistRepository
import com.novosynk.domain.repository.SongRepository
import com.novosynk.domain.repository.StreamRepository
import kotlinx.coroutines.CoroutineScope

expect fun createMediaServiceHandler(
    dataStoreManager: DataStoreManager,
    songRepository: SongRepository,
    streamRepository: StreamRepository,
    localPlaylistRepository: LocalPlaylistRepository,
    analyticsRepository: AnalyticsRepository,
    coroutineScope: CoroutineScope,
): MediaPlayerHandler