package com.novosynk.domain.data.model.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseRelease(
    val id: String? = null,
    val version: String = "",
    val title: String = "",
    val description: String? = null,
    @SerialName("download_url")
    val downloadUrl: String? = null,
    @SerialName("apk_url")
    val apkUrl: String? = null,
    @SerialName("version_code")
    val versionCode: Int? = null,
    @SerialName("release_notes")
    val releaseNotes: String? = null,
    @SerialName("is_published")
    val isPublished: Boolean = true,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("is_force_update")
    val isForceUpdate: Boolean = false,
    @SerialName("is_forced")
    val isForced: Boolean = false,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("published_at")
    val publishedAt: String? = null,
) {
    val finalDownloadUrl: String
        get() = downloadUrl?.takeIf { it.isNotBlank() } ?: apkUrl ?: ""

    val finalNotes: String?
        get() = description?.takeIf { it.isNotBlank() } ?: releaseNotes

    val isForcedUpdate: Boolean
        get() = isForceUpdate || isForced
}

@Serializable
data class ReleaseWhatsNew(
    val id: String? = null,
    @SerialName("release_id")
    val releaseId: String? = null,
    val content: String,
    @SerialName("sort_order")
    val sortOrder: Int = 0,
    @SerialName("created_at")
    val createdAt: String? = null,
)

@Serializable
data class SupabaseNotification(
    val id: String? = null,
    val title: String,
    val description: String,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("is_published")
    val isPublished: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("published_at")
    val publishedAt: String? = null,
)

@Serializable
data class BugReport(
    val id: String? = null,
    val title: String,
    val description: String,
    @SerialName("name")
    val userName: String = "Guest",
    @SerialName("email")
    val userEmail: String? = null,
    @SerialName("app_version")
    val appVersion: String,
    @SerialName("android_version")
    val androidVersion: String,
    @SerialName("device_manufacturer")
    val deviceManufacturer: String,
    @SerialName("device_model")
    val deviceModel: String,
    @SerialName("user_id")
    val userId: String? = null,
    val status: String = "new",
    @SerialName("screenshot_path")
    val screenshotPath: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
)

@Serializable
data class BugReportAttachment(
    val id: String? = null,
    @SerialName("bug_report_id")
    val bugReportId: String,
    @SerialName("storage_path")
    val storagePath: String,
    @SerialName("file_name")
    val fileName: String? = null,
    @SerialName("file_size")
    val fileSize: Long? = null,
    @SerialName("mime_type")
    val mimeType: String? = "image/jpeg",
    @SerialName("created_at")
    val createdAt: String? = null,
)
