package com.novosynk.data.repository

import com.novosynk.domain.data.model.update.UpdateData
import com.novosynk.domain.repository.SupabaseRepository
import com.novosynk.domain.repository.UpdateRepository
import com.novosynk.domain.utils.Resource
import com.novosynk.kotlinytmusicscraper.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

internal class UpdateRepositoryImpl(
    private val youTube: YouTube,
    private val supabaseRepository: SupabaseRepository,
) : UpdateRepository {
    override fun checkForGithubReleaseUpdate(): Flow<Resource<UpdateData>> =
        flow {
            supabaseRepository.getLatestRelease().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val pair = result.data
                        if (pair != null) {
                            val release = pair.first
                            val whatsNew = pair.second
                            val bodyText = if (whatsNew.isNotEmpty()) {
                                whatsNew.joinToString("\n") { "• ${it.content}" }
                            } else {
                                release.finalNotes ?: ""
                            }
                            emit(
                                Resource.Success(
                                    UpdateData(
                                        tagName = release.version,
                                        releaseTime = release.createdAt,
                                        body = bodyText,
                                        apkDownloadUrl = release.finalDownloadUrl.takeIf { it.isNotBlank() },
                                        apkFileName = "Musico-${release.version}.apk",
                                        title = release.title.ifBlank { "Musico ${release.version} is Available" },
                                        isForceUpdate = release.isForcedUpdate,
                                        whatsNewList = whatsNew,
                                    ),
                                ),
                            )
                        } else {
                            emit(Resource.Error("No release data found"))
                        }
                    }

                    is Resource.Error -> {
                        emit(Resource.Error(result.message ?: "Failed to check update"))
                    }
                }
            }
        }.flowOn(Dispatchers.IO)

    override fun checkForFdroidUpdate(): Flow<Resource<UpdateData>> =
        flow {
            youTube
                .checkForFdroidUpdate()
                .onSuccess { response ->
                    val latestVersion = response.packages.maxBy { it.versionCode }
                    emit(
                        Resource.Success(
                            UpdateData(
                                tagName = latestVersion.versionName,
                                releaseTime = null,
                                body =
                                    $$"""
                                    ### Update via F-Droid, changelogs: 
                                    - https://github.com/novosynk/MusicoCode/blob/main/fastlane/metadata/android/en-US/changelogs/$${latestVersion.versionCode}.txt
                                    """.trimIndent(),
                            ),
                        ),
                    )
                }.onFailure {
                    emit(Resource.Error<UpdateData>(it.localizedMessage ?: "Unknown error"))
                }
        }.flowOn(Dispatchers.IO)

    override fun getFdroidSigningKeys(): Flow<Resource<List<String>>> =
        flow {
            youTube
                .getFdroidSigningKeys()
                .onSuccess { emit(Resource.Success(it)) }
                .onFailure { emit(Resource.Error<List<String>>(it.localizedMessage ?: "Unknown error")) }
        }.flowOn(Dispatchers.IO)
}
