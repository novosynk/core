package com.novosynk.domain.data.model.update

import com.novosynk.domain.data.model.supabase.ReleaseWhatsNew

data class UpdateData(
    val tagName: String,
    val releaseTime: String?,
    val body: String,
    val apkDownloadUrl: String? = null,
    val apkFileName: String? = null,
    val apkSize: Long? = null,
    val title: String? = null,
    val isForceUpdate: Boolean = false,
    val whatsNewList: List<ReleaseWhatsNew> = emptyList(),
)
