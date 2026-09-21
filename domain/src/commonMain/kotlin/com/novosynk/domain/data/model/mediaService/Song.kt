package com.novosynk.domain.data.model.mediaService

import com.novosynk.domain.data.model.searchResult.songs.Album
import com.novosynk.domain.data.model.searchResult.songs.Artist
import com.novosynk.domain.data.model.searchResult.songs.Thumbnail

data class Song(
    val title: String?,
    val artists: List<Artist>?,
    val duration: Long,
    val lyrics: Any,
    val album: Album,
    val videoId: String,
    val thumbnail: Thumbnail?,
    val isLocal: Boolean,
)