package com.lightningstudio.watchrss.data.bili

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lightningstudio.watchrss.sdk.bili.BiliAccount
import com.lightningstudio.watchrss.sdk.bili.BiliClient
import com.lightningstudio.watchrss.sdk.bili.BiliCookies
import com.lightningstudio.watchrss.sdk.bili.BiliFavoriteFolder
import com.lightningstudio.watchrss.sdk.bili.BiliFavoritePage
import com.lightningstudio.watchrss.sdk.bili.BiliFeedPage
import com.lightningstudio.watchrss.sdk.bili.BiliHistoryCursor
import com.lightningstudio.watchrss.sdk.bili.BiliHistoryPage
import com.lightningstudio.watchrss.sdk.bili.BiliItem
import com.lightningstudio.watchrss.sdk.bili.BiliPlayUrl
import com.lightningstudio.watchrss.sdk.bili.BiliResult
import com.lightningstudio.watchrss.sdk.bili.BiliSdkConfig
import com.lightningstudio.watchrss.sdk.bili.BiliToViewPage
import com.lightningstudio.watchrss.sdk.bili.BiliVideoDetail
import com.lightningstudio.watchrss.sdk.bili.EncryptedBiliAccountStore
import com.lightningstudio.watchrss.sdk.bili.QrPollResult
import com.lightningstudio.watchrss.sdk.bili.TvQrCode
import com.lightningstudio.watchrss.sdk.bili.WebQrCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val FEED_CACHE_JSON = stringPreferencesKey("bili_feed_cache_json")
private val FEED_CACHE_AT = longPreferencesKey("bili_feed_cache_at")
private const val FEED_CACHE_LIMIT = 50

class BiliRepository(
    context: Context,
    private val dataStore: DataStore<Preferences>
) {
    private val accountStore = EncryptedBiliAccountStore(context)
    private val client = BiliClient(BiliSdkConfig(), accountStore)

    suspend fun isLoggedIn(): Boolean {
        val account = accountStore.read()
        return !account?.cookies?.get("SESSDATA").isNullOrBlank()
    }

    suspend fun readAccount(): BiliAccount? = accountStore.read()

    suspend fun clearAccount() {
        accountStore.write(BiliAccount())
    }

    suspend fun applyCookieHeader(rawCookie: String): Result<Unit> {
        val cookies = BiliCookies.parseCookieHeader(rawCookie)
        if (cookies.isEmpty()) {
            return Result.failure(IllegalArgumentException("缺少有效 Cookie"))
        }
        client.auth.applyCookies(cookies)
        return Result.success(Unit)
    }

    suspend fun requestWebQrCode(): WebQrCode? = client.auth.requestWebQrCode()

    suspend fun requestTvQrCode(): TvQrCode? = client.auth.requestTvQrCode()

    suspend fun pollWebQrCode(qrKey: String): QrPollResult = client.auth.pollWebQrCode(qrKey)

    suspend fun pollTvQrCode(authCode: String): QrPollResult = client.auth.pollTvQrCode(authCode)

    suspend fun fetchFeed(): BiliResult<BiliFeedPage> = client.feed.fetchDefaultFeed()

    suspend fun readFeedCache(): List<BiliItem> = withContext(Dispatchers.IO) {
        val raw = dataStore.data.first()[FEED_CACHE_JSON].orEmpty()
        if (raw.isBlank()) return@withContext emptyList()
        parseFeedCache(raw)
    }

    suspend fun writeFeedCache(items: List<BiliItem>) {
        val trimmed = items.take(FEED_CACHE_LIMIT)
        val raw = buildFeedCacheJson(trimmed)
        dataStore.edit { preferences ->
            preferences[FEED_CACHE_JSON] = raw
            preferences[FEED_CACHE_AT] = System.currentTimeMillis()
        }
    }

    suspend fun fetchVideoDetail(aid: Long? = null, bvid: String? = null): BiliResult<BiliVideoDetail> {
        return client.video.fetchView(aid = aid, bvid = bvid, useWbi = true)
    }

    suspend fun fetchPlayUrlMp4(
        cid: Long,
        aid: Long? = null,
        bvid: String? = null,
        qn: Int = 32
    ): BiliResult<BiliPlayUrl> {
        return client.play.fetchMp4Url(
            cid = cid,
            aid = aid,
            bvid = bvid,
            qn = qn
        )
    }

    suspend fun like(aid: Long, like: Boolean): BiliResult<Unit> = client.action.like(aid, like)

    suspend fun coin(aid: Long, multiply: Int = 1, selectLike: Boolean = false): BiliResult<Boolean> {
        return client.action.coin(aid, multiply, selectLike)
    }

    suspend fun triple(aid: Long): BiliResult<com.lightningstudio.watchrss.sdk.bili.BiliTripleResult> {
        return client.action.triple(aid)
    }

    suspend fun favorite(aid: Long, add: Boolean): BiliResult<Boolean> {
        val folderId = defaultFavoriteFolderId()
            ?: return BiliResult(-1, "missing_favorite_folder")
        val addIds = if (add) listOf(folderId) else emptyList()
        val delIds = if (add) emptyList() else listOf(folderId)
        return client.action.favorite(aid, addMediaIds = addIds, delMediaIds = delIds)
    }

    suspend fun addToView(aid: Long? = null, bvid: String? = null): BiliResult<Unit> {
        return client.history.addToView(aid, bvid)
    }

    suspend fun fetchToView(): BiliResult<BiliToViewPage> = client.history.fetchToView()

    suspend fun fetchHistory(cursor: BiliHistoryCursor? = null): BiliResult<BiliHistoryPage> {
        return client.history.fetchHistory(cursor)
    }

    suspend fun fetchFavoriteFolders(): BiliResult<List<BiliFavoriteFolder>> {
        val mid = currentUserMid() ?: return BiliResult(-1, "missing_mid")
        return client.favorite.listFolders(mid)
    }

    suspend fun fetchFavoriteItems(mediaId: Long, pn: Int = 1, ps: Int = 20): BiliResult<BiliFavoritePage> {
        return client.favorite.listResources(mediaId = mediaId, pn = pn, ps = ps)
    }

    suspend fun buildPlayHeaders(): Map<String, String> {
        val account = accountStore.read()
        val cookies = account?.cookies?.takeIf { it.isNotEmpty() }
        val headers = mutableMapOf(
            "User-Agent" to client.config.webUserAgent,
            "Referer" to client.config.webReferer
        )
        if (cookies != null) {
            headers["Cookie"] = cookies.entries.joinToString("; ") { (k, v) -> "$k=$v" }
        }
        return headers
    }

    fun shareLink(bvid: String?, aid: Long?): String? {
        return when {
            !bvid.isNullOrBlank() -> "https://www.bilibili.com/video/$bvid"
            aid != null -> "https://www.bilibili.com/video/av$aid"
            else -> null
        }
    }

    private suspend fun currentUserMid(): Long? {
        val account = accountStore.read()
        val raw = account?.cookies?.get("DedeUserID") ?: return null
        return raw.toLongOrNull()
    }

    private suspend fun defaultFavoriteFolderId(): Long? {
        val folders = fetchFavoriteFolders()
        if (!folders.isSuccess) return null
        val first = folders.data?.firstOrNull() ?: return null
        return first.id ?: first.fid
    }

    private fun buildFeedCacheJson(items: List<BiliItem>): String {
        val array = JSONArray()
        items.forEach { item ->
            array.put(toCacheJson(item))
        }
        return array.toString()
    }

    private fun toCacheJson(item: BiliItem): JSONObject {
        val obj = JSONObject()
        item.aid?.let { obj.put("aid", it) }
        item.bvid?.let { obj.put("bvid", it) }
        item.cid?.let { obj.put("cid", it) }
        item.title?.let { obj.put("title", it) }
        item.cover?.let { obj.put("cover", it) }
        item.duration?.let { obj.put("duration", it) }
        item.pubdate?.let { obj.put("pubdate", it) }
        item.owner?.name?.let { obj.put("owner", it) }
        item.stat?.view?.let { obj.put("view", it) }
        item.stat?.like?.let { obj.put("like", it) }
        item.stat?.danmaku?.let { obj.put("danmaku", it) }
        return obj
    }

    private fun parseFeedCache(raw: String): List<BiliItem> {
        return runCatching {
            val array = JSONArray(raw)
            val items = mutableListOf<BiliItem>()
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                items.add(fromCacheJson(obj))
            }
            items
        }.getOrDefault(emptyList())
    }

    private fun fromCacheJson(obj: JSONObject): BiliItem {
        val aid = obj.optLong("aid", -1L).takeIf { it > 0 }
        val bvid = obj.optString("bvid", "").takeIf { it.isNotBlank() }
        val cid = obj.optLong("cid", -1L).takeIf { it > 0 }
        val title = obj.optString("title", "").takeIf { it.isNotBlank() }
        val cover = obj.optString("cover", "").takeIf { it.isNotBlank() }
        val duration = obj.optInt("duration", -1).takeIf { it > 0 }
        val pubdate = obj.optLong("pubdate", -1L).takeIf { it > 0 }
        val ownerName = obj.optString("owner", "").takeIf { it.isNotBlank() }
        val view = obj.optLong("view", -1L).takeIf { it >= 0 }
        val like = obj.optLong("like", -1L).takeIf { it >= 0 }
        val danmaku = obj.optLong("danmaku", -1L).takeIf { it >= 0 }
        val owner = ownerName?.let { com.lightningstudio.watchrss.sdk.bili.BiliOwner(name = it) }
        val stat = if (view != null || like != null || danmaku != null) {
            com.lightningstudio.watchrss.sdk.bili.BiliStat(
                view = view,
                like = like,
                danmaku = danmaku
            )
        } else {
            null
        }
        return BiliItem(
            aid = aid,
            bvid = bvid,
            cid = cid,
            title = title,
            cover = cover,
            duration = duration,
            pubdate = pubdate,
            owner = owner,
            stat = stat
        )
    }
}
