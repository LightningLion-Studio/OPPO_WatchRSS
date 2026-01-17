package com.lightningstudio.watchrss.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RssItemDao {
    @Query("SELECT * FROM rss_items WHERE channelId = :channelId ORDER BY fetchedAt DESC, id DESC")
    fun observeItems(channelId: Long): Flow<List<RssItemEntity>>

    @Query("SELECT * FROM rss_items WHERE id = :id")
    fun observeItem(id: Long): Flow<RssItemEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItems(items: List<RssItemEntity>): List<Long>

    @Query("UPDATE rss_items SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE rss_items SET isRead = 1 WHERE channelId = :channelId")
    suspend fun markReadByChannel(channelId: Long)

    @Query("SELECT channelId, COUNT(*) as unreadCount FROM rss_items WHERE isRead = 0 GROUP BY channelId")
    fun observeUnreadCounts(): Flow<List<RssChannelUnreadCount>>

    @Query("SELECT SUM(contentSizeBytes) FROM rss_items")
    fun observeTotalCacheBytes(): Flow<Long?>

    @Query("SELECT SUM(contentSizeBytes) FROM rss_items")
    suspend fun getTotalCacheBytes(): Long?

    @Query("SELECT id, contentSizeBytes FROM rss_items ORDER BY fetchedAt ASC, id ASC")
    suspend fun loadOldestItems(): List<RssItemSize>

    @Query("DELETE FROM rss_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM rss_items WHERE channelId = :channelId")
    suspend fun deleteByChannel(channelId: Long)
}

data class RssItemSize(
    val id: Long,
    val contentSizeBytes: Long
)

data class RssChannelUnreadCount(
    val channelId: Long,
    val unreadCount: Int
)
