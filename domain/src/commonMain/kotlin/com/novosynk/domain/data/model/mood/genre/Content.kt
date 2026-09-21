package com.novosynk.domain.data.model.mood.genre

import com.novosynk.domain.data.model.searchResult.songs.Thumbnail
import com.novosynk.domain.data.type.HomeContentType

data class Content(
    val playlistBrowseId: String,
    val thumbnail: List<Thumbnail>?,
    val title: Title,
) : HomeContentType