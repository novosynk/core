package com.novosynk.domain.repository

import com.novosynk.domain.data.entities.LyricsEntity
import com.novosynk.domain.data.entities.TranslatedLyricsEntity
import com.novosynk.domain.data.model.browse.album.Track
import com.novosynk.domain.data.model.browse.artist.ArtistLogo
import com.novosynk.domain.data.model.canvas.CanvasResult
import com.novosynk.domain.data.model.metadata.Lyrics
import com.novosynk.domain.manager.DataStoreManager
import com.novosynk.domain.utils.Resource
import kotlinx.coroutines.flow.Flow

interface LyricsCanvasRepository {
    fun getSavedLyrics(videoId: String): Flow<LyricsEntity?>

    suspend fun insertLyrics(lyricsEntity: LyricsEntity)

    suspend fun insertTranslatedLyrics(translatedLyrics: TranslatedLyricsEntity)

    fun getSavedTranslatedLyrics(
        videoId: String,
        language: String,
    ): Flow<TranslatedLyricsEntity?>

    suspend fun removeTranslatedLyrics(
        videoId: String,
        language: String,
    )

    fun getYouTubeCaption(
        preferLang: String,
        videoId: String,
    ): Flow<Resource<Pair<Lyrics, Lyrics?>>>

    fun getCanvas(
        dataStoreManager: DataStoreManager,
        videoId: String,
        duration: Int,
    ): Flow<Resource<CanvasResult>>

    suspend fun updateCanvasUrl(
        videoId: String,
        canvasUrl: String,
    )

    suspend fun updateCanvasThumbUrl(
        videoId: String,
        canvasThumbUrl: String,
    )

    fun getSpotifyLyrics(
        dataStoreManager: DataStoreManager,
        query: String,
        duration: Int?,
    ): Flow<Resource<Lyrics>>

    fun getLrclibLyricsData(
        sartist: String,
        strack: String,
        duration: Int?,
    ): Flow<Resource<Lyrics>>

    fun getBetterLyrics(
        artist: String,
        track: String,
        duration: Int?,
    ): Flow<Resource<Lyrics>>

    /** Fetch the artist's name-logo image + dominant color from the hidden catalog. */
    fun getArtistLogo(artistName: String): Flow<Resource<ArtistLogo>>

    fun getAITranslationLyrics(
        lyrics: Lyrics,
        targetLanguage: String,
    ): Flow<Resource<Lyrics>>

    fun getMusicoLyrics(videoId: String): Flow<Resource<Lyrics>>

    fun getMusicoTranslatedLyrics(
        videoId: String,
        language: String,
    ): Flow<Resource<Lyrics>>

    fun voteMusicoLyrics(
        lyricsId: String,
        upvote: Boolean,
    ): Flow<Resource<String>>

    fun voteMusicoTranslatedLyrics(
        translatedLyricsId: String,
        upvote: Boolean,
    ): Flow<Resource<String>>

    fun insertMusicoLyrics(
        dataStoreManager: DataStoreManager,
        track: Track,
        duration: Int,
        lyrics: Lyrics,
    ): Flow<Resource<String>>

    fun insertMusicoTranslatedLyrics(
        dataStoreManager: DataStoreManager,
        track: Track,
        translatedLyrics: Lyrics,
        language: String,
    ): Flow<Resource<String>>
}