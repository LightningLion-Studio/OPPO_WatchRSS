package com.lightningstudio.watchrss.data.rss

import android.content.Context
import android.net.Uri
import com.lightningstudio.watchrss.data.db.OfflineMediaDao
import com.lightningstudio.watchrss.data.db.OfflineMediaEntity
import com.lightningstudio.watchrss.data.db.RssChannelDao
import com.lightningstudio.watchrss.data.db.RssChannelEntity
import com.lightningstudio.watchrss.data.db.RssItemDao
import com.lightningstudio.watchrss.data.db.RssItemEntity
import com.lightningstudio.watchrss.data.db.SavedEntryDao
import com.lightningstudio.watchrss.data.db.SavedEntryEntity
import com.lightningstudio.watchrss.data.db.SavedRssItem
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
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.io.File

class DefaultRssRepository(
    private val appContext: Context,
    private val channelDao: RssChannelDao,
    private val itemDao: RssItemDao,
    private val savedEntryDao: SavedEntryDao,
    private val offlineMediaDao: OfflineMediaDao,
    private val settingsRepository: SettingsRepository
) : RssRepository {
    private val offlineRoot: File = File(appContext.filesDir, "offline/rss").apply {
        if (!exists()) {
            mkdirs()
        }
    }

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

    private val downloadClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .callTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
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
        combine(
            channelDao.observeChannel(channelId),
            itemDao.observeUnreadCount(channelId)
        ) { channel, unreadCount ->
            channel?.toModel(unreadCount)
        }

    override fun observeItems(channelId: Long): Flow<List<RssItem>> =
        itemDao.observeItems(channelId).map { items ->
            items.map { it.toModel() }
        }

    override fun observeItem(itemId: Long): Flow<RssItem?> =
        itemDao.observeItem(itemId).map { it?.toModel() }

    override fun observeCacheUsageBytes(): Flow<Long> =
        itemDao.observeTotalCacheBytes().map { it ?: 0L }

    override fun observeSavedItems(saveType: SaveType): Flow<List<SavedItem>> =
        savedEntryDao.observeSavedItems(saveType.name).map { items ->
            items.map { it.toModel() }
        }

    override fun observeSavedState(itemId: Long): Flow<SavedState> =
        savedEntryDao.observeByItemId(itemId).map { entries ->
            val types = entries.map { it.saveType }.toSet()
            SavedState(
                isFavorite = SaveType.FAVORITE.name in types,
                isWatchLater = SaveType.WATCH_LATER.name in types
            )
        }

    override fun observeOfflineMedia(itemId: Long): Flow<List<OfflineMedia>> =
        offlineMediaDao.observeByItemId(itemId).map { list ->
            list.map { it.toModel() }
        }

    override suspend fun ensureBuiltinChannels() = withContext(Dispatchers.IO) {
        if (settingsRepository.builtinChannelsInitialized.first()) return@withContext
        val now = System.currentTimeMillis()
        BuiltinChannelType.values().forEachIndexed { index, type ->
            val timestamp = now - index
            val entity = RssChannelEntity(
                url = type.url,
                title = type.title,
                description = type.description,
                imageUrl = null,
                lastFetchedAt = null,
                createdAt = timestamp,
                sortOrder = timestamp,
                isPinned = false
            )
            channelDao.insertChannel(entity)
        }
        settingsRepository.setBuiltinChannelsInitialized(true)
    }

    override suspend fun previewChannel(url: String): Result<AddRssPreview> = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeUrl(url)
        if (!isValidUrl(normalizedUrl)) {
            return@withContext Result.failure(IllegalArgumentException("URL 不合法"))
        }

        val builtinType = builtinTypeFromInputUrl(normalizedUrl)
        if (builtinType != null) {
            val existing = channelDao.getChannelByUrl(builtinType.url)
            if (existing != null) {
                return@withContext Result.success(AddRssPreview.Existing(existing.toModel(0)))
            }
            val preview = RssChannelPreview(
                url = builtinType.url,
                title = builtinType.title,
                description = builtinType.description,
                imageUrl = null,
                siteUrl = null,
                items = emptyList(),
                isBuiltin = true
            )
            return@withContext Result.success(AddRssPreview.Ready(preview))
        }

        val existing = channelDao.getChannelByUrl(normalizedUrl)
        if (existing != null) {
            return@withContext Result.success(AddRssPreview.Existing(existing.toModel(0)))
        }

        runCatching {
            val parsed = parser.getRssChannel(normalizedUrl)
            val preview = RssChannelPreview(
                url = normalizedUrl,
                title = channelTitle(parsed, normalizedUrl),
                description = parsed.description?.trim()?.ifEmpty { null },
                imageUrl = parsed.image?.url?.trim()?.ifEmpty { null },
                siteUrl = parsed.link?.trim()?.ifEmpty { null },
                items = parsed.items.map { it.toPreviewItem() },
                isBuiltin = false
            )
            AddRssPreview.Ready(preview)
        }.mapError()
    }

    override suspend fun confirmAddChannel(preview: RssChannelPreview): Result<RssChannel> =
        withContext(Dispatchers.IO) {
            if (preview.isBuiltin) {
                val builtinType = BuiltinChannelType.fromUrl(preview.url)
                    ?: return@withContext Result.failure(IllegalArgumentException("不支持的内置频道"))
                val existing = channelDao.getChannelByUrl(builtinType.url)
                if (existing != null) {
                    return@withContext Result.success(existing.toModel(0))
                }
                val now = System.currentTimeMillis()
                val channel = RssChannelEntity(
                    url = builtinType.url,
                    title = builtinType.title,
                    description = builtinType.description,
                    imageUrl = null,
                    lastFetchedAt = null,
                    createdAt = now,
                    sortOrder = now,
                    isPinned = false
                )
                val channelId = channelDao.insertChannel(channel)
                val storedChannel = if (channelId > 0) {
                    channel.copy(id = channelId)
                } else {
                    channelDao.getChannelByUrl(builtinType.url) ?: channel
                }
                return@withContext Result.success(storedChannel.toModel(0))
            }

            val existing = channelDao.getChannelByUrl(preview.url)
            if (existing != null) {
                return@withContext Result.success(existing.toModel(0))
            }

            runCatching {
                val fetchedAt = System.currentTimeMillis()
                val channel = RssChannelEntity(
                    url = preview.url,
                    title = preview.title,
                    description = preview.description,
                    imageUrl = preview.imageUrl,
                    lastFetchedAt = fetchedAt,
                    createdAt = fetchedAt,
                    sortOrder = fetchedAt,
                    isPinned = false
                )
                val channelId = channelDao.insertChannel(channel)
                val storedChannel = if (channelId > 0) {
                    channel.copy(id = channelId)
                } else {
                    channelDao.getChannelByUrl(preview.url) ?: channel
                }
                val items = preview.items.map { item ->
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
            }.mapError()
        }

    override suspend fun addChannel(url: String): Result<RssChannel> = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeUrl(url)
        if (!isValidUrl(normalizedUrl)) {
            return@withContext Result.failure(IllegalArgumentException("URL 不合法"))
        }

        val builtinType = builtinTypeFromInputUrl(normalizedUrl)
        if (builtinType != null) {
            val existing = channelDao.getChannelByUrl(builtinType.url)
            if (existing != null) {
                return@withContext Result.success(existing.toModel(0))
            }
            val now = System.currentTimeMillis()
            val channel = RssChannelEntity(
                url = builtinType.url,
                title = builtinType.title,
                description = builtinType.description,
                imageUrl = null,
                lastFetchedAt = null,
                createdAt = now,
                sortOrder = now,
                isPinned = false
            )
            val channelId = channelDao.insertChannel(channel)
            val storedChannel = if (channelId > 0) {
                channel.copy(id = channelId)
            } else {
                channelDao.getChannelByUrl(builtinType.url) ?: channel
            }
            return@withContext Result.success(storedChannel.toModel(0))
        }

        val existing = channelDao.getChannelByUrl(normalizedUrl)
        if (existing != null) {
            return@withContext Result.success(existing.toModel(0))
        }

        val preview = previewChannel(normalizedUrl).getOrElse { return@withContext Result.failure(it) }
        if (preview is AddRssPreview.Existing) {
            return@withContext Result.success(preview.channel)
        }
        confirmAddChannel((preview as AddRssPreview.Ready).preview)
    }

    override suspend fun refreshChannel(channelId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val channel = channelDao.getChannel(channelId)
            ?: return@withContext Result.failure(IllegalArgumentException("频道不存在"))
        if (BuiltinChannelType.fromUrl(channel.url) != null) {
            return@withContext Result.success(Unit)
        }

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
        }.mapError()
    }

    override suspend fun markItemRead(itemId: Long) {
        withContext(Dispatchers.IO) {
            itemDao.markRead(itemId)
        }
    }

    override suspend fun toggleFavorite(itemId: Long): Result<SavedState> =
        toggleSaved(itemId, SaveType.FAVORITE)

    override suspend fun toggleWatchLater(itemId: Long): Result<SavedState> =
        toggleSaved(itemId, SaveType.WATCH_LATER)

    override suspend fun toggleLike(itemId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        val item = itemDao.getItem(itemId)
            ?: return@withContext Result.failure(IllegalArgumentException("内容不存在"))
        val newValue = !item.isLiked
        itemDao.updateLiked(itemId, newValue)
        Result.success(newValue)
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
            val entries = offlineMediaDao.getByChannelId(channelId)
            entries.forEach { entry ->
                val path = entry.localPath ?: return@forEach
                runCatching { File(path).delete() }
            }
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

        val oldest = itemDao.loadOldestItemsExcludingSaved()
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
        if (url.length > 2048) return false
        if (url.any { it.isWhitespace() }) return false
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return false
        return !uri.host.isNullOrBlank()
    }

    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return trimmed
        val lower = trimmed.lowercase()
        if (lower.startsWith("http://") || lower.startsWith("https://")) return trimmed
        if (trimmed.contains("://")) return trimmed
        return if (looksLikeUrlWithoutScheme(trimmed)) {
            "http://$trimmed"
        } else {
            trimmed
        }
    }

    private fun looksLikeUrlWithoutScheme(input: String): Boolean {
        if (input.startsWith("/")) return false
        if (input.any { it.isWhitespace() }) return false
        val hostPort = input.substringBefore('/').substringBefore('?').substringBefore('#')
        if (hostPort.isEmpty()) return false
        val host = hostPort.substringBefore(':')
        if (host.equals("localhost", ignoreCase = true)) return true
        if (host.startsWith('.') || host.endsWith('.')) return false
        if (!host.contains('.')) return false
        return host.all { it.isLetterOrDigit() || it == '.' || it == '-' }
    }

    private fun builtinTypeFromInputUrl(url: String): BuiltinChannelType? {
        val host = runCatching { Uri.parse(url).host }.getOrNull()
        return BuiltinChannelType.fromHost(host)
    }

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
            isLiked = false,
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

    private fun ParsedItem.toPreviewItem(): RssPreviewItem = RssPreviewItem(
        title = title?.trim()?.ifEmpty { null },
        description = description?.trim()?.ifEmpty { null },
        content = content?.trim()?.ifEmpty { null },
        link = link?.trim()?.ifEmpty { null },
        guid = guid?.trim()?.ifEmpty { null },
        pubDate = pubDate?.trim()?.ifEmpty { null },
        imageUrl = image?.trim()?.ifEmpty { null },
        audioUrl = audio?.trim()?.ifEmpty { null },
        videoUrl = video?.trim()?.ifEmpty { null }
    )

    private fun RssPreviewItem.toEntity(
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
        val safeImage = imageUrl?.trim()?.ifEmpty { null }
        val safeAudio = audioUrl?.trim()?.ifEmpty { null }
        val safeVideo = videoUrl?.trim()?.ifEmpty { null }
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
            isLiked = false,
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

    private suspend fun toggleSaved(itemId: Long, saveType: SaveType): Result<SavedState> =
        withContext(Dispatchers.IO) {
            val item = itemDao.getItem(itemId)
                ?: return@withContext Result.failure(IllegalArgumentException("内容不存在"))
            val existing = savedEntryDao.getByItemId(itemId)
            val hasType = existing.any { it.saveType == saveType.name }
            if (hasType) {
                savedEntryDao.delete(itemId, saveType.name)
            } else {
                savedEntryDao.insert(
                    SavedEntryEntity(
                        itemId = itemId,
                        saveType = saveType.name,
                        createdAt = System.currentTimeMillis()
                    )
                )
                runCatching { downloadOfflineMediaForItem(item) }
            }
            if (savedEntryDao.countByItemId(itemId) == 0) {
                deleteOfflineMediaForItem(itemId)
            }
            val updated = savedEntryDao.getByItemId(itemId)
            Result.success(buildSavedState(updated))
        }

    private fun buildSavedState(entries: List<SavedEntryEntity>): SavedState {
        val types = entries.map { it.saveType }.toSet()
        return SavedState(
            isFavorite = SaveType.FAVORITE.name in types,
            isWatchLater = SaveType.WATCH_LATER.name in types
        )
    }

    private suspend fun downloadOfflineMediaForItem(item: RssItemEntity) {
        val refs = RssMediaExtractor.extract(item.toModel())
            .filterNot { it.url.startsWith("data:", ignoreCase = true) }
        if (refs.isEmpty()) return
        val itemDir = File(offlineRoot, item.id.toString()).apply { mkdirs() }
        val now = System.currentTimeMillis()
        val entities = mutableListOf<OfflineMediaEntity>()
        refs.forEachIndexed { index, ref ->
            val exists = offlineMediaDao.findByOrigin(item.id, ref.url)
            if (exists != null) return@forEachIndexed
            val file = File(itemDir, buildFileName(ref.type, index, ref.url))
            val localPath = downloadToFile(ref.url, file)
            entities.add(
                OfflineMediaEntity(
                    itemId = item.id,
                    mediaType = ref.type.name,
                    originUrl = ref.url,
                    localPath = localPath,
                    createdAt = now
                )
            )
        }
        if (entities.isNotEmpty()) {
            offlineMediaDao.insertAll(entities)
        }
    }

    private suspend fun deleteOfflineMediaForItem(itemId: Long) {
        val entries = offlineMediaDao.getByItemId(itemId)
        entries.forEach { entry ->
            val path = entry.localPath ?: return@forEach
            runCatching { File(path).delete() }
        }
        offlineMediaDao.deleteByItemId(itemId)
    }

    private fun buildFileName(type: OfflineMediaType, index: Int, url: String): String {
        val clean = url.substringBefore('?').substringBefore('#')
        val extension = clean.substringAfterLast('.', "")
        val suffix = if (extension.isNotEmpty() && extension.length <= 6) {
            ".$extension"
        } else {
            if (type == OfflineMediaType.VIDEO) ".mp4" else ".jpg"
        }
        return "${type.name.lowercase()}_${index}$suffix"
    }

    private fun downloadToFile(url: String, file: File): String? {
        return try {
            downloadClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) {
                    return null
                }
                response.body?.byteStream()?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            if (file.exists() && file.length() > 0) file.absolutePath else null
        } catch (e: Exception) {
            null
        }
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

    private fun SavedRssItem.toModel(): SavedItem = SavedItem(
        item = item.toModel(),
        channelTitle = channelTitle,
        savedAt = savedAt,
        saveType = SaveType.valueOf(saveType)
    )

    private fun OfflineMediaEntity.toModel(): OfflineMedia = OfflineMedia(
        itemId = itemId,
        type = OfflineMediaType.valueOf(mediaType),
        originUrl = originUrl,
        localPath = localPath
    )

    private fun <T> Result<T>.mapError(): Result<T> {
        val error = exceptionOrNull() ?: return this
        val message = when (error) {
            is java.net.UnknownHostException -> "网络不可用"
            is java.net.SocketTimeoutException -> "请求超时"
            is java.net.ConnectException -> "网络不可用"
            is javax.net.ssl.SSLException -> "网络不可用"
            is java.io.EOFException -> "解析失败"
            is org.xml.sax.SAXParseException -> "不是有效的RSS/Atom"
            is IllegalArgumentException -> error.message ?: "参数错误"
            else -> error.message ?: "解析失败"
        }
        return Result.failure(IllegalStateException(message))
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
        isLiked = isLiked,
        fetchedAt = fetchedAt
    )
}
