package com.novosynk.data.repository

import com.novosynk.data.db.datasource.LocalDataSource
import com.novosynk.data.extension.getFullDataFromDB
import com.novosynk.data.parser.parseArtistData
import com.novosynk.domain.data.entities.ArtistEntity
import com.novosynk.domain.data.model.browse.artist.ArtistBrowse
import com.novosynk.domain.repository.ArtistRepository
import com.novosynk.domain.utils.Resource
import com.novosynk.kotlinytmusicscraper.YouTube
import com.novosynk.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime

internal class ArtistRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val youTube: YouTube,
) : ArtistRepository {
    override fun getAllArtists(limit: Int): Flow<List<ArtistEntity>> =
        flow {
            emit(localDataSource.getAllArtists(limit))
        }.flowOn(Dispatchers.IO)

    override fun getArtistById(id: String): Flow<ArtistEntity?> =
        flow {
            emit(localDataSource.getArtist(id))
        }.flowOn(Dispatchers.IO)

    override suspend fun insertArtist(artistEntity: ArtistEntity) =
        withContext(Dispatchers.IO) {
            localDataSource.insertArtist(artistEntity)
        }

    override suspend fun updateArtistImage(
        channelId: String,
        thumbnail: String,
    ) = withContext(
        Dispatchers.Main,
    ) {
        localDataSource.updateArtistImage(
            channelId,
            thumbnail,
        )
    }

    override suspend fun updateArtistNameLogo(
        channelId: String,
        nameLogoUrl: String?,
        nameLogoColor: String?,
    ) = withContext(Dispatchers.IO) {
        localDataSource.updateArtistNameLogo(channelId, nameLogoUrl, nameLogoColor)
    }

    /**
     * Unfollowing also drops what only existed to serve the follow.
     *
     * Flipping the flag was all this ever did, so an artist's notifications and their new-releases
     * tracking row survived every unfollow — and once the unfollowed `artist` row is itself swept by
     * `SongRepository.clearHistoryAndOrphanedSongs`, they have nothing left to point back at.
     */
    override suspend fun updateFollowedStatus(
        channelId: String,
        followedStatus: Int,
    ) = withContext(
        Dispatchers.Main,
    ) {
        localDataSource.updateFollowed(followedStatus, channelId)
        if (followedStatus == 0) {
            localDataSource.deleteNotificationsByChannelId(channelId)
            localDataSource.deleteFollowedArtistSingleAndAlbum(channelId)
        }
    }

    override fun getFollowedArtists(): Flow<List<ArtistEntity>> =
        flow {
            emit(
                getFullDataFromDB { limit, offset ->
                    localDataSource.getFollowedArtists(limit, offset)
                },
            )
        }.flowOn(Dispatchers.IO)

    override suspend fun updateArtistInLibrary(
        inLibrary: LocalDateTime,
        channelId: String,
    ) = withContext(Dispatchers.Main) {
        localDataSource.updateArtistInLibrary(
            inLibrary,
            channelId,
        )
    }

    override fun getArtistData(channelId: String): Flow<Resource<ArtistBrowse>> =
        flow {
            runCatching {
                youTube
                    .artist(channelId)
                    .onSuccess { result ->
                        emit(Resource.Success<ArtistBrowse>(parseArtistData(result)))
                    }.onFailure { e ->
                        Logger.d("Artist", "Error: ${e.message}")
                        emit(Resource.Error<ArtistBrowse>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)
}