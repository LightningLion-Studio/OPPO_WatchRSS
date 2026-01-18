package com.lightningstudio.watchrss.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.lightningstudio.watchrss.data.db.WatchRssDatabase
import com.lightningstudio.watchrss.data.rss.DefaultRssRepository
import com.lightningstudio.watchrss.data.rss.RssRepository
import com.lightningstudio.watchrss.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

interface AppContainer {
    val rssRepository: RssRepository
    val settingsRepository: SettingsRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database: WatchRssDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            WatchRssDatabase::class.java,
            "watchrss.db"
        ).addMigrations(
            WatchRssDatabase.MIGRATION_1_2,
            WatchRssDatabase.MIGRATION_2_3,
            WatchRssDatabase.MIGRATION_3_4
        )
            .build()
    }

    override val settingsRepository: SettingsRepository by lazy {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { appContext.preferencesDataStoreFile("settings.preferences_pb") }
        )
        SettingsRepository(dataStore)
    }

    override val rssRepository: RssRepository by lazy {
        DefaultRssRepository(
            appContext = appContext,
            channelDao = database.rssChannelDao(),
            itemDao = database.rssItemDao(),
            savedEntryDao = database.savedEntryDao(),
            offlineMediaDao = database.offlineMediaDao(),
            settingsRepository = settingsRepository,
            appScope = appScope
        )
    }
}
