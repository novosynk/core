package com.novosynk.domain.repository

import com.novosynk.domain.data.model.supabase.BugReport
import com.novosynk.domain.data.model.supabase.ReleaseWhatsNew
import com.novosynk.domain.data.model.supabase.SupabaseNotification
import com.novosynk.domain.data.model.supabase.SupabaseRelease
import com.novosynk.domain.utils.Resource
import kotlinx.coroutines.flow.Flow

interface SupabaseRepository {
    fun getLatestRelease(): Flow<Resource<Pair<SupabaseRelease, List<ReleaseWhatsNew>>>>
    fun getActiveNotifications(): Flow<Resource<List<SupabaseNotification>>>
    fun submitBugReport(
        report: BugReport,
        screenshotBytes: ByteArray? = null,
        screenshotFileName: String? = null,
    ): Flow<Resource<Boolean>>
}
