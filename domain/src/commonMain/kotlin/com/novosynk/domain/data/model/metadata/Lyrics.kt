package com.novosynk.domain.data.model.metadata

import kotlinx.serialization.Serializable

@Serializable
data class Lyrics(
    val error: Boolean = false,
    val lines: List<Line>?,
    val syncType: String?,
    val MusicoLyrics: MusicoLyrics? = null,
) {
    val simpMusicLyrics: MusicoLyrics? get() = MusicoLyrics
}

@Serializable
data class MusicoLyrics(
    val id: String,
    val vote: Int,
)