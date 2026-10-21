package com.novosynk.data.di

import com.novosynk.common.AppIdentity
import com.novosynk.common.Config.SERVICE_SCOPE
import com.novosynk.data.io.fileDir
import com.novosynk.data.repository.AccountRepositoryImpl
import com.novosynk.data.repository.AlbumRepositoryImpl
import com.novosynk.data.repository.AnalyticsRepositoryImpl
import com.novosynk.data.repository.ArtistRepositoryImpl
import com.novosynk.data.repository.AutoEqRepositoryImpl
import com.novosynk.data.lyrics.LyricsRomanizerRepositoryImpl
import com.novosynk.data.repository.CommonRepositoryImpl
import com.novosynk.data.repository.HomeRepositoryImpl
import com.novosynk.data.repository.ImportRepositoryImpl
import com.novosynk.data.repository.LocalPlaylistRepositoryImpl
import com.novosynk.data.repository.LyricsCanvasRepositoryImpl
import com.novosynk.data.repository.PlaylistRepositoryImpl
import com.novosynk.data.repository.PodcastRepositoryImpl
import com.novosynk.data.repository.SearchRepositoryImpl
import com.novosynk.data.repository.SongRepositoryImpl
import com.novosynk.data.repository.StreamRepositoryImpl
import com.novosynk.data.repository.SupabaseRepositoryImpl
import com.novosynk.domain.repository.SupabaseRepository
import com.novosynk.data.repository.TasteRepositoryImpl
import com.novosynk.data.repository.UpdateRepositoryImpl
import com.novosynk.domain.repository.AccountRepository
import com.novosynk.domain.repository.AlbumRepository
import com.novosynk.domain.repository.AnalyticsRepository
import com.novosynk.domain.repository.ArtistRepository
import com.novosynk.domain.repository.AutoEqRepository
import com.novosynk.domain.repository.LyricsRomanizerRepository
import com.novosynk.domain.repository.CommonRepository
import com.novosynk.domain.repository.HomeRepository
import com.novosynk.domain.repository.ImportRepository
import com.novosynk.domain.repository.LocalPlaylistRepository
import com.novosynk.domain.repository.LyricsCanvasRepository
import com.novosynk.domain.repository.PlaylistRepository
import com.novosynk.domain.repository.PodcastRepository
import com.novosynk.domain.repository.SearchRepository
import com.novosynk.domain.repository.SongRepository
import com.novosynk.domain.repository.StreamRepository
import com.novosynk.domain.repository.TasteRepository
import com.novosynk.domain.repository.UpdateRepository
import org.koin.core.qualifier.named
import org.koin.dsl.module

val repositoryModule =
    module {
        single<AccountRepository>(createdAtStart = true) {
            AccountRepositoryImpl(get(), get())
        }

        single<AlbumRepository>(createdAtStart = true) {
            AlbumRepositoryImpl(get(), get())
        }

        single<ArtistRepository>(createdAtStart = true) {
            ArtistRepositoryImpl(get(), get(), get())
        }

        single<CommonRepository>(createdAtStart = true) {
            CommonRepositoryImpl(get(named(SERVICE_SCOPE)), get(), get(), get(), get(), get()).apply {
                this.init("${fileDir()}/ytdlp-cookie.txt", get(), get<AppIdentity>().isDevBuild)
            }
        }

        // Lazy for the same reason its client is: the picker is the only thing that wants it.
        single<AutoEqRepository> {
            AutoEqRepositoryImpl(get(), get())
        }

        // Lazy: constructing it costs a few File.length() calls (plus, while the pack is missing,
        // one background read of the stored selection), but the kuromoji dictionary behind it is
        // loaded on first Japanese line, or right after a download installs it, and never at
        // startup — so this must NOT be createdAtStart, or every launch pays for a feature most
        // listeners leave off. The path is where Android keeps the downloaded ipadic pack (the
        // APK no longer bundles it); Desktop and iOS ignore it.
        single<LyricsRomanizerRepository> {
            LyricsRomanizerRepositoryImpl("${fileDir()}/kuromoji-ipadic", get())
        }

        single<HomeRepository>(createdAtStart = true) {
            HomeRepositoryImpl(get(), get())
        }

        single<ImportRepository>(createdAtStart = true) {
            ImportRepositoryImpl(get())
        }

        single<LocalPlaylistRepository>(createdAtStart = true) {
            LocalPlaylistRepositoryImpl(get(), get())
        }

        single<LyricsCanvasRepository>(createdAtStart = true) {
            LyricsCanvasRepositoryImpl(get(), get(), get(), get(), get())
        }

        single<PlaylistRepository>(createdAtStart = true) {
            PlaylistRepositoryImpl(get(), get(), get())
        }

        single<PodcastRepository>(createdAtStart = true) {
            PodcastRepositoryImpl(get(), get())
        }

        single<SearchRepository>(createdAtStart = true) {
            SearchRepositoryImpl(get(), get())
        }

        single<SongRepository>(createdAtStart = true) {
            SongRepositoryImpl(get(), get(), get())
        }

        single<StreamRepository>(createdAtStart = true) {
            StreamRepositoryImpl(get(), get())
        }

        single<UpdateRepository>(createdAtStart = true) {
            UpdateRepositoryImpl(get(), get())
        }

        single<SupabaseRepository>(createdAtStart = true) {
            SupabaseRepositoryImpl()
        }

        single<AnalyticsRepository>(createdAtStart = true) {
            AnalyticsRepositoryImpl(get())
        }

        // Lazy: only the taste card in Library asks for it.
        single<TasteRepository> {
            TasteRepositoryImpl(get(), get())
        }
    }