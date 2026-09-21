package com.novosynk.data.repository

import com.novosynk.domain.data.model.supabase.BugReport
import com.novosynk.domain.data.model.supabase.BugReportAttachment
import com.novosynk.domain.data.model.supabase.ReleaseWhatsNew
import com.novosynk.domain.data.model.supabase.SupabaseNotification
import com.novosynk.domain.data.model.supabase.SupabaseRelease
import com.novosynk.domain.repository.SupabaseRepository
import com.novosynk.domain.utils.Resource
import com.novosynk.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

import com.novosynk.ktorext.getEngine

class SupabaseRepositoryImpl(
    private val supabaseUrl: String = "https://ycfjldxfpcwafrktxklw.supabase.co",
    private val supabaseKey: String = "sb_publishable_hreY9nmUHTzIP_KcoS3a-g_iYpf5AlB",
) : SupabaseRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val client = HttpClient(getEngine()) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    override fun getLatestRelease(): Flow<Resource<Pair<SupabaseRelease, List<ReleaseWhatsNew>>>> =
        flow {
            try {
                // Fetch releases that are published OR active
                val response = client.get("$supabaseUrl/rest/v1/app_releases?or=(is_published.eq.true,is_active.eq.true)&order=created_at.desc&select=*") {
                    header("apikey", supabaseKey)
                    header("Authorization", "Bearer $supabaseKey")
                    contentType(ContentType.Application.Json)
                }

                if (!response.status.isSuccess()) {
                    val fallbackResponse = client.get("$supabaseUrl/rest/v1/app_releases?order=created_at.desc&select=*") {
                        header("apikey", supabaseKey)
                        header("Authorization", "Bearer $supabaseKey")
                        contentType(ContentType.Application.Json)
                    }
                    if (!fallbackResponse.status.isSuccess()) {
                        emit(Resource.Error("Failed to fetch releases: HTTP ${response.status.value}"))
                        return@flow
                    }
                    val fallbackReleases: List<SupabaseRelease> = json.decodeFromString(fallbackResponse.bodyAsText())
                    val validReleases = fallbackReleases.filter { it.isPublished || it.isActive }
                    if (validReleases.isEmpty()) {
                        emit(Resource.Error("No published releases found"))
                        return@flow
                    }
                    val sortedReleases = validReleases.sortedWith { r1, r2 ->
                        compareSemanticVersion(r1.version, r2.version)
                    }
                    val latestRelease = sortedReleases.last()
                    emit(Resource.Success(Pair(latestRelease, emptyList())))
                    return@flow
                }

                val bodyText = response.bodyAsText()
                val releases: List<SupabaseRelease> = json.decodeFromString(bodyText)

                if (releases.isEmpty()) {
                    emit(Resource.Error("No published releases found"))
                    return@flow
                }

                // Semantic version selection (highest semantic version)
                val sortedReleases = releases.sortedWith { r1, r2 ->
                    compareSemanticVersion(r1.version, r2.version)
                }
                val latestRelease = sortedReleases.last()

                // Fetch What's New items for this release (by release_id or by version)
                var whatsNewList: List<ReleaseWhatsNew> = emptyList()
                try {
                    val releaseId = latestRelease.id
                    val url = if (releaseId != null) {
                        "$supabaseUrl/rest/v1/release_whats_new?release_id=eq.$releaseId&order=sort_order.asc&select=*"
                    } else {
                        "$supabaseUrl/rest/v1/release_whats_new?version=eq.${latestRelease.version}&order=sort_order.asc&select=*"
                    }
                    val wnResponse = client.get(url) {
                        header("apikey", supabaseKey)
                        header("Authorization", "Bearer $supabaseKey")
                        contentType(ContentType.Application.Json)
                    }
                    if (wnResponse.status.isSuccess()) {
                        whatsNewList = json.decodeFromString(wnResponse.bodyAsText())
                    }
                } catch (e: Exception) {
                    Logger.w("SupabaseRepo", "Failed to fetch whats_new: ${e.message}")
                }

                emit(Resource.Success(Pair(latestRelease, whatsNewList)))
            } catch (e: Exception) {
                Logger.e("SupabaseRepo", "Error fetching latest release: ${e.message}")
                emit(Resource.Error(e.message ?: "Unknown network error"))
            }
        }.flowOn(Dispatchers.IO)

    override fun getActiveNotifications(): Flow<Resource<List<SupabaseNotification>>> =
        flow {
            try {
                // Fetch notifications that are active OR published
                val response = client.get("$supabaseUrl/rest/v1/app_notifications?or=(is_active.eq.true,is_published.eq.true)&order=created_at.desc&select=*") {
                    header("apikey", supabaseKey)
                    header("Authorization", "Bearer $supabaseKey")
                    contentType(ContentType.Application.Json)
                }

                if (response.status.isSuccess()) {
                    val notifications: List<SupabaseNotification> = json.decodeFromString(response.bodyAsText())
                    emit(Resource.Success(notifications))
                } else {
                    // Fallback to all notifications if query filter fails
                    val fallbackResponse = client.get("$supabaseUrl/rest/v1/app_notifications?order=created_at.desc&select=*") {
                        header("apikey", supabaseKey)
                        header("Authorization", "Bearer $supabaseKey")
                        contentType(ContentType.Application.Json)
                    }
                    if (fallbackResponse.status.isSuccess()) {
                        val notifications: List<SupabaseNotification> = json.decodeFromString(fallbackResponse.bodyAsText())
                        val filtered = notifications.filter { it.isActive || it.isPublished }
                        emit(Resource.Success(filtered))
                    } else {
                        emit(Resource.Error("Failed to fetch notifications: HTTP ${response.status.value}"))
                    }
                }
            } catch (e: Exception) {
                Logger.e("SupabaseRepo", "Error fetching notifications: ${e.message}")
                emit(Resource.Error(e.message ?: "Unknown error"))
            }
        }.flowOn(Dispatchers.IO)

    override fun submitBugReport(
        report: BugReport,
        screenshotBytes: ByteArray?,
        screenshotFileName: String?,
    ): Flow<Resource<Boolean>> =
        flow {
            try {
                // Step 1: Insert into `bug_reports` with id = null so Postgres generates valid UUID
                val finalReport = report.copy(id = null)
                val bugReportPayload = json.encodeToString(BugReport.serializer(), finalReport)
                val insertBugResponse = client.post("$supabaseUrl/rest/v1/bug_reports") {
                    header("apikey", supabaseKey)
                    header("Authorization", "Bearer $supabaseKey")
                    header("Prefer", "return=representation")
                    contentType(ContentType.Application.Json)
                    setBody(bugReportPayload)
                }

                if (!insertBugResponse.status.isSuccess()) {
                    val errorBody = insertBugResponse.bodyAsText()
                    Logger.e("SupabaseRepo", "Bug report insert failed: HTTP ${insertBugResponse.status.value} - $errorBody")
                    emit(Resource.Error("Failed to submit bug report: HTTP ${insertBugResponse.status.value}"))
                    return@flow
                }

                // Step 2: If screenshot exists, attach directly via Base64 Data URI
                if (screenshotBytes != null && screenshotBytes.isNotEmpty()) {
                    try {
                        val responseText = insertBugResponse.bodyAsText()
                        val insertedList: List<BugReport> = json.decodeFromString(responseText)
                        val createdBugId = insertedList.firstOrNull()?.id
                        if (createdBugId != null) {
                            val ext = screenshotFileName?.substringAfterLast('.', "jpg") ?: "jpg"
                            val mimeType = if (ext.equals("png", ignoreCase = true)) "image/png" else "image/jpeg"
                            val base64Data = kotlin.io.encoding.Base64.encode(screenshotBytes)
                            val dataUri = "data:$mimeType;base64,$base64Data"

                            val attachment = BugReportAttachment(
                                bugReportId = createdBugId,
                                storagePath = dataUri,
                                fileName = screenshotFileName ?: "screenshot.$ext",
                                fileSize = screenshotBytes.size.toLong(),
                                mimeType = mimeType,
                            )
                            val attachmentPayload = json.encodeToString(BugReportAttachment.serializer(), attachment)
                            val insertAttachmentResponse = client.post("$supabaseUrl/rest/v1/bug_report_attachments") {
                                header("apikey", supabaseKey)
                                header("Authorization", "Bearer $supabaseKey")
                                header("Prefer", "return=minimal")
                                contentType(ContentType.Application.Json)
                                setBody(attachmentPayload)
                            }

                            if (!insertAttachmentResponse.status.isSuccess()) {
                                Logger.w("SupabaseRepo", "Attachment record insert warning: HTTP ${insertAttachmentResponse.status.value}")
                            }
                        }
                    } catch (e: Exception) {
                        Logger.w("SupabaseRepo", "Failed to insert bug attachment metadata: ${e.message}")
                    }
                }

                emit(Resource.Success(true))
            } catch (e: Exception) {
                Logger.e("SupabaseRepo", "Error submitting bug report: ${e.message}")
                emit(Resource.Error(e.message ?: "Failed to submit bug report"))
            }
        }.flowOn(Dispatchers.IO)

    companion object {
        fun compareSemanticVersion(v1: String, v2: String): Int {
            val clean1 = v1.removePrefix("v").substringBefore("-").trim()
            val clean2 = v2.removePrefix("v").substringBefore("-").trim()
            val parts1 = clean1.split(".").mapNotNull { it.toIntOrNull() }
            val parts2 = clean2.split(".").mapNotNull { it.toIntOrNull() }
            val maxLen = maxOf(parts1.size, parts2.size)
            for (i in 0 until maxLen) {
                val p1 = parts1.getOrElse(i) { 0 }
                val p2 = parts2.getOrElse(i) { 0 }
                if (p1 != p2) return p1.compareTo(p2)
            }
            return 0
        }
    }
}
