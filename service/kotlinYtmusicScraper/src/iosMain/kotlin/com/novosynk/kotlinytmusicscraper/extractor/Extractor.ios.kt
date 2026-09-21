package com.novosynk.kotlinytmusicscraper.extractor

import com.novosynk.kotlinytmusicscraper.models.SongItem
import com.novosynk.kotlinytmusicscraper.models.response.DownloadProgress

actual class Extractor {
    actual fun init() {
    }

    actual fun logIn(cookie: String?) {}

    actual fun newPipePlayer(videoId: String): List<Pair<Int, String>> = emptyList()

    actual fun mergeAudioVideoDownload(filePath: String): DownloadProgress = DownloadProgress.failed("Not supported on iOS")

    actual fun saveAudioWithThumbnail(
        filePath: String,
        track: SongItem,
    ): DownloadProgress = DownloadProgress.failed("Not supported on iOS")
}