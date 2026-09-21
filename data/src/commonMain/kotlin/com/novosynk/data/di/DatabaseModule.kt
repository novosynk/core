package com.novosynk.data.di

import DatabaseDao
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.novosynk.data.dataStore.DataStoreManagerImpl
import com.novosynk.data.dataStore.createDataStoreInstance
import com.novosynk.data.db.Converters
import com.novosynk.data.db.MusicDatabase
import com.novosynk.data.db.datasource.AnalyticsDatasource
import com.novosynk.data.db.datasource.LocalDataSource
import com.novosynk.data.db.getDatabaseBuilder
import com.novosynk.domain.manager.DataStoreManager
import com.novosynk.kotlinytmusicscraper.YouTube
import com.novosynk.spotify.Spotify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.dsl.module
import com.novosynk.aiservice.AiClient
import com.novosynk.lyrics.MusicoLyricsClient
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
val databaseModule =
    module {
        single(createdAtStart = true) {
            Converters()
        }
        // Database
        single(createdAtStart = true) {
            getDatabaseBuilder(
                get<Converters>()
            )
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }
        // DatabaseDao
        single(createdAtStart = true) {
            get<MusicDatabase>().getDatabaseDao()
        }
        // LocalDataSource
        single(createdAtStart = true) {
            LocalDataSource(get<DatabaseDao>(), get<MusicDatabase>())
        }
        // AnalyticsDatasource
        single(createdAtStart = true) {
            AnalyticsDatasource(get<DatabaseDao>())
        }
        // Datastore
        single(createdAtStart = true) {
            createDataStoreInstance()
        }
        // DatastoreManager
        single<DataStoreManager>(createdAtStart = true) {
            DataStoreManagerImpl(get<DataStore<Preferences>>())
        }

        // Move YouTube from Singleton to Koin DI
        single(createdAtStart = true) {
            YouTube()
        }

        single(createdAtStart = true) {
            Spotify()
        }

        single(createdAtStart = true) {
            AiClient()
        }

        single(createdAtStart = true) {
            MusicoLyricsClient()
        }
    }