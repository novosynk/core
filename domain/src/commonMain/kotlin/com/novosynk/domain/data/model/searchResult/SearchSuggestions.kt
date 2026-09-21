package com.novosynk.domain.data.model.searchResult

import com.novosynk.domain.data.type.SearchResultType

data class SearchSuggestions(
    val queries: List<String>,
    val recommendedItems: List<SearchResultType>,
)