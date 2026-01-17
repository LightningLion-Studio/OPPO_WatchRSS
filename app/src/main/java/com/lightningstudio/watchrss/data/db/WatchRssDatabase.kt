package com.lightningstudio.watchrss.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        RssChannelEntity::class,
        RssItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class WatchRssDatabase : RoomDatabase() {
    abstract fun rssChannelDao(): RssChannelDao
    abstract fun rssItemDao(): RssItemDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE rss_channels ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE rss_channels ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "UPDATE rss_channels SET sortOrder = createdAt"
                )
            }
        }
    }
}
