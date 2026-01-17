package com.lightningstudio.watchrss.data.rss

import android.net.Uri
import com.lightningstudio.watchrss.data.db.RssChannelDao
import com.lightningstudio.watchrss.data.db.RssChannelEntity
import com.lightningstudio.watchrss.data.db.RssItemDao
import com.lightningstudio.watchrss.data.db.RssItemEntity
import com.lightningstudio.watchrss.data.settings.SettingsRepository
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.RssParserBuilder
import com.prof18.rssparser.model.RssChannel as ParsedChannel
import com.prof18.rssparser.model.RssItem as ParsedItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class DefaultRssRepository(
    private val channelDao: RssChannelDao,
    private val itemDao: RssItemDao,
    private val settingsRepository: SettingsRepository
) : RssRepository {
    private val parser: RssParser by lazy {
        val client = OkHttpClient.Builder()
            .callTimeout(20, TimeUnit.SECONDS)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
        RssParserBuilder(
            callFactory = client,
            charset = Charsets.UTF_8
        ).build()
    }

    override fun observeChannels(): Flow<List<RssChannel>> =
        combine(
            channelDao.observeChannels(),
            itemDao.observeUnreadCounts()
        ) { channels, unreadCounts ->
            val unreadMap = unreadCounts.associate { it.channelId to it.unreadCount }
            channels.map { channel ->
                channel.toModel(unreadMap[channel.id] ?: 0)
            }
        }

    override fun observeChannel(channelId: Long): Flow<RssChannel?> =
        channelDao.observeChannel(channelId).map { it?.toModel(0) }

    override fun observeItems(channelId: Long): Flow<List<RssItem>> =
        itemDao.observeItems(channelId).map { items ->
            items.map { it.toModel() }
        }

    override fun observeItem(itemId: Long): Flow<RssItem?> =
        itemDao.observeItem(itemId).map { it?.toModel() }

    override fun observeCacheUsageBytes(): Flow<Long> =
        itemDao.observeTotalCacheBytes().map { it ?: 0L }

    override suspend fun addChannel(url: String): Result<RssChannel> = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeUrl(url)
        if (!isValidUrl(normalizedUrl)) {
            return@withContext Result.failure(IllegalArgumentException("URL 不合法"))
        }

        val existing = channelDao.getChannelByUrl(normalizedUrl)
        if (existing != null) {
            return@withContext Result.success(existing.toModel(0))
        }

        runCatching {
            val fetchedAt = System.currentTimeMillis()
            val parsed = parser.getRssChannel(normalizedUrl)
            val channel = RssChannelEntity(
                url = normalizedUrl,
                title = channelTitle(parsed, normalizedUrl),
                description = parsed.description?.trim()?.ifEmpty { null },
                imageUrl = parsed.image?.url?.trim()?.ifEmpty { null },
                lastFetchedAt = fetchedAt,
                createdAt = fetchedAt,
                sortOrder = fetchedAt,
                isPinned = false
            )
            val channelId = channelDao.insertChannel(channel)
            val storedChannel = if (channelId > 0) {
                channel.copy(id = channelId)
            } else {
                channelDao.getChannelByUrl(normalizedUrl) ?: channel
            }

            val items = parsed.items.map { item ->
                item.toEntity(
                    channelId = storedChannel.id,
                    isRead = true,
                    fetchedAt = fetchedAt
                )
            }
            if (items.isNotEmpty()) {
                itemDao.insertItems(items)
            }
            trimCacheToLimit()
            storedChannel.toModel(0)
        }
    }

    override suspend fun refreshChannel(channelId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val channel = channelDao.getChannel(channelId)
            ?: return@withContext Result.failure(IllegalArgumentException("频道不存在"))

        runCatching {
            val fetchedAt = System.currentTimeMillis()
            val parsed = parser.getRssChannel(channel.url)
            val items = parsed.items.map { item ->
                item.toEntity(
                    channelId = channelId,
                    isRead = false,
                    fetchedAt = fetchedAt
                )
            }
            if (items.isNotEmpty()) {
                itemDao.insertItems(items)
            }
            channelDao.updateChannel(channel.copy(
                title = channelTitle(parsed, channel.url),
                description = parsed.description?.trim()?.ifEmpty { null },
                imageUrl = parsed.image?.url?.trim()?.ifEmpty { null },
                lastFetchedAt = fetchedAt
            ))
            trimCacheToLimit()
        }
    }

    override suspend fun markItemRead(itemId: Long) {
        withContext(Dispatchers.IO) {
            itemDao.markRead(itemId)
        }
    }

    override suspend fun markChannelRead(channelId: Long) {
        withContext(Dispatchers.IO) {
            itemDao.markReadByChannel(channelId)
        }
    }

    override suspend fun moveChannelToTop(channelId: Long) {
        withContext(Dispatchers.IO) {
            val channel = channelDao.getChannel(channelId) ?: return@withContext
            channelDao.updateChannel(channel.copy(sortOrder = System.currentTimeMillis()))
        }
    }

    override suspend fun setChannelPinned(channelId: Long, pinned: Boolean) {
        withContext(Dispatchers.IO) {
            val channel = channelDao.getChannel(channelId) ?: return@withContext
            val newOrder = if (pinned) System.currentTimeMillis() else channel.sortOrder
            channelDao.updateChannel(channel.copy(isPinned = pinned, sortOrder = newOrder))
        }
    }

    override suspend fun deleteChannel(channelId: Long) {
        withContext(Dispatchers.IO) {
            channelDao.deleteChannel(channelId)
        }
    }

    override suspend fun trimCacheToLimit() {
        withContext(Dispatchers.IO) {
            val limit = settingsRepository.cacheLimitBytes.first()
            enforceCacheLimit(limit)
        }
    }

    private suspend fun enforceCacheLimit(limitBytes: Long) {
        if (limitBytes <= 0) return
        var total = itemDao.getTotalCacheBytes() ?: 0L
        if (total <= limitBytes) return

        val oldest = itemDao.loadOldestItems()
        val toDelete = mutableListOf<Long>()
        for (item in oldest) {
            if (total <= limitBytes) break
            total -= item.contentSizeBytes
            toDelete.add(item.id)
        }
        if (toDelete.isNotEmpty()) {
            itemDao.deleteByIds(toDelete)
        }
    }

    private fun isValidUrl(url: String): Boolean {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase()
        return scheme == "http" || scheme == "https"
    }

    private fun normalizeUrl(url: String): String = url.trim()

    private fun channelTitle(parsed: ParsedChannel, url: String): String {
        val title = parsed.title?.trim().orEmpty()
        if (title.isNotEmpty()) return title
        val host = runCatching { Uri.parse(url).host }.getOrNull().orEmpty()
        return host.ifEmpty { url }
    }

    private fun ParsedItem.toEntity(
        channelId: Long,
        isRead: Boolean,
        fetchedAt: Long
    ): RssItemEntity {
        val safeTitle = title?.trim().takeUnless { it.isNullOrEmpty() }
            ?: link?.trim().takeUnless { it.isNullOrEmpty() }
            ?: "未命名内容"
        val safeDescription = description?.trim()?.ifEmpty { null }
        val safeContent = content?.trim()?.ifEmpty { null }
        val safeLink = link?.trim()?.ifEmpty { null }
        val safeGuid = guid?.trim()?.ifEmpty { null }
        val safePubDate = pubDate?.trim()?.ifEmpty { null }
        val safeImage = image?.trim()?.ifEmpty { null }
        val safeAudio = audio?.trim()?.ifEmpty { null }
        val safeVideo = video?.trim()?.ifEmpty { null }
        val dedupKey = computeDedupKey(safeGuid, safeLink, safeTitle)

        return RssItemEntity(
            channelId = channelId,
            title = safeTitle,
            description = safeDescription,
            content = safeContent,
            link = safeLink,
            guid = safeGuid,
            pubDate = safePubDate,
            imageUrl = safeImage,
            audioUrl = safeAudio,
            videoUrl = safeVideo,
            isRead = isRead,
            dedupKey = dedupKey,
            fetchedAt = fetchedAt,
            contentSizeBytes = estimateContentSize(
                safeTitle,
                safeDescription,
                safeContent,
                safeLink,
                safeImage,
                safeAudio,
                safeVideo
            )
        )
    }

    private fun computeDedupKey(guid: String?, link: String?, title: String): String = when {
        !guid.isNullOrBlank() -> "guid:$guid"
        !link.isNullOrBlank() -> "link:$link"
        else -> "title:$title"
    }

    private fun estimateContentSize(vararg parts: String?): Long {
        var total = 0L
        for (part in parts) {
            if (!part.isNullOrEmpty()) {
                total += part.toByteArray(Charsets.UTF_8).size
            }
        }
        return total
    }

    private fun RssChannelEntity.toModel(unreadCount: Int): RssChannel = RssChannel(
        id = id,
        url = url,
        title = title,
        description = description,
        imageUrl = imageUrl,
        lastFetchedAt = lastFetchedAt,
        sortOrder = sortOrder,
        isPinned = isPinned,
        unreadCount = unreadCount
    )

    private fun RssItemEntity.toModel(): RssItem = RssItem(
        id = id,
        channelId = channelId,
        title = title,
        description = description,
        content = content,
        link = link,
        pubDate = pubDate,
        imageUrl = imageUrl,
        audioUrl = audioUrl,
        videoUrl = videoUrl,
        isRead = isRead,
        fetchedAt = fetchedAt
    )
}
