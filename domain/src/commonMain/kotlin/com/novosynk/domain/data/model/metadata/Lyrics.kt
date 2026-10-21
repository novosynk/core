package com.novosynk.domain.data.model.metadata

import kotlinx.serialization.Serializable

@Serializable
data class Lyrics(
    val error: Boolean = false,
    val lines: List<Line>?,
    val syncType: String?,
    val simpMusicLyrics: SimpMusicLyrics? = null,
) {
    val MusicoLyrics: SimpMusicLyrics? get() = simpMusicLyrics
}

@Serializable
data class SimpMusicLyrics(
    val id: String,
    val vote: Int,
)

typealias MusicoLyrics = SimpMusicLyrics
