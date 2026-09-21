package com.novosynk.domain.data.model.browse.artist

import com.novosynk.domain.data.model.searchResult.songs.Thumbnail
import com.novosynk.domain.data.type.HomeContentType

data class ResultSingle(
    val browseId: String,
    val thumbnails: List<Thumbnail>,
    val title: String,
    val year: String,
) : HomeContentType