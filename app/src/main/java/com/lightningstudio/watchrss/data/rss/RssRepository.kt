package com.lightningstudio.watchrss.data.rss

import kotlinx.coroutines.flow.Flow

interface RssRepository {
    fun observeChannels(): Flow<List<RssChannel>>
    fun observeChannel(channelId: Long): Flow<RssChannel?>
    fun observeItems(channelId: Long): Flow<List<RssItem>>
    fun observeItem(itemId: Long): Flow<RssItem?>
    fun observeCacheUsageBytes(): Flow<Long>
    fun observeSavedItems(saveType: SaveType): Flow<List<SavedItem>>
    fun observeSavedState(itemId: Long): Flow<SavedState>
    fun observeOfflineMedia(itemId: Long): Flow<List<OfflineMedia>>

    suspend fun ensureBuiltinChannels()
    suspend fun previewChannel(url: String): Result<AddRssPreview>
    suspend fun confirmAddChannel(preview: RssChannelPreview): Result<RssChannel>
    suspend fun addChannel(url: String): Result<RssChannel>
    suspend fun refreshChannel(channelId: Long): Result<Unit>
    suspend fun markItemRead(itemId: Long)
    suspend fun toggleFavorite(itemId: Long): Result<SavedState>
    suspend fun toggleWatchLater(itemId: Long): Result<SavedState>
    suspend fun toggleLike(itemId: Long): Result<Boolean>
    suspend fun markChannelRead(channelId: Long)
    suspend fun moveChannelToTop(channelId: Long)
    suspend fun setChannelPinned(channelId: Long, pinned: Boolean)
    suspend fun deleteChannel(channelId: Long)
    suspend fun trimCacheToLimit()
}
