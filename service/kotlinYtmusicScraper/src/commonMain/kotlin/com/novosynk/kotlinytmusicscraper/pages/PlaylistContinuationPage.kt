package com.novosynk.kotlinytmusicscraper.pages

import com.novosynk.kotlinytmusicscraper.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)