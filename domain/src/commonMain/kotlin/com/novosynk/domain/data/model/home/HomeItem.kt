package com.novosynk.domain.data.model.home

import com.novosynk.domain.data.model.searchResult.songs.Thumbnail

data class HomeItem(
    val contents: List<Content?>,
    val title: String,
    val subtitle: String? = null,
    val thumbnail: List<Thumbnail>? = null,
    val channelId: String? = null,
    val moreEndpoint: MoreEndpoint? = null,
) {
    data class MoreEndpoint(
        val browseId: String,
        val params: String? = null,
        val pageType: String? = null,
    ) {
        companion object {
            const val PAGE_TYPE_ALBUM = "MUSIC_PAGE_TYPE_ALBUM"
            const val PAGE_TYPE_AUDIOBOOK = "MUSIC_PAGE_TYPE_AUDIOBOOK"
            const val PAGE_TYPE_PLAYLIST = "MUSIC_PAGE_TYPE_PLAYLIST"
            const val PAGE_TYPE_ARTIST = "MUSIC_PAGE_TYPE_ARTIST"
            const val PAGE_TYPE_USER_CHANNEL = "MUSIC_PAGE_TYPE_USER_CHANNEL"
            const val PAGE_TYPE_PODCAST = "MUSIC_PAGE_TYPE_PODCAST"
        }
    }
}