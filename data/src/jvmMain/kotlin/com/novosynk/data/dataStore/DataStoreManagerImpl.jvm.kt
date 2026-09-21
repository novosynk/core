package com.novosynk.data.dataStore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.novosynk.common.SETTINGS_FILENAME
import com.novosynk.data.io.getHomeFolderPath
import createDataStore
import java.io.File

actual fun createDataStoreInstance(): DataStore<Preferences> = createDataStore(
    producePath = {
        val file = File(getHomeFolderPath(listOf(".Musico")), "$SETTINGS_FILENAME.preferences_pb")
        file.absolutePath
    }
)