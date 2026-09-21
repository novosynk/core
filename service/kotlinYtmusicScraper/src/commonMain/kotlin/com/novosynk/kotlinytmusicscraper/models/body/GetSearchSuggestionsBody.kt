package com.novosynk.kotlinytmusicscraper.models.body

import com.novosynk.kotlinytmusicscraper.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetSearchSuggestionsBody(
    val context: Context,
    val input: String,
)