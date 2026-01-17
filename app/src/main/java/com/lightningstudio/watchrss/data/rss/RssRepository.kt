package com.lightningstudio.watchrss.data.rss

import kotlinx.coroutines.flow.Flow

interface RssRepository {
    fun observeChannels(): Flow<List<RssChannel>>
    fun observeChannel(channelId: Long): Flow<RssChannel?>
    fun observeItems(channelId: Long): Flow<List<RssItem>>
    fun observeItem(itemId: Long): Flow<RssItem?>
    fun observeCacheUsageBytes(): Flow<Long>

    suspend fun addChannel(url: String): Result<RssChannel>
    suspend fun refreshChannel(channelId: Long): Result<Unit>
    suspend fun markItemRead(itemId: Long)
    suspend fun markChannelRead(channelId: Long)
    suspend fun moveChannelToTop(channelId: Long)
    suspend fun setChannelPinned(channelId: Long, pinned: Boolean)
    suspend fun deleteChannel(channelId: Long)
    suspend fun trimCacheToLimit()
}
