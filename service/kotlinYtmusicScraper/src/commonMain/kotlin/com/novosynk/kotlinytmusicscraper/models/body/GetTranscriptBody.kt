package com.novosynk.kotlinytmusicscraper.models.body

import com.novosynk.kotlinytmusicscraper.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetTranscriptBody(
    val context: Context,
    val params: String,
)