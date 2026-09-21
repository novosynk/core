package com.novosynk.kotlinytmusicscraper.pages

import com.novosynk.kotlinytmusicscraper.models.AlbumItem
import com.novosynk.kotlinytmusicscraper.models.VideoItem

data class ExplorePage(
    val released: List<AlbumItem>,
    val musicVideo: List<VideoItem>,
)