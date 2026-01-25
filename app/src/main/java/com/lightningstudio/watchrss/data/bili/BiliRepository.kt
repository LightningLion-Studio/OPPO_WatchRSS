package com.lightningstudio.watchrss.data.bili

import android.content.Context
import com.lightningstudio.watchrss.sdk.bili.BiliAccount
import com.lightningstudio.watchrss.sdk.bili.BiliClient
import com.lightningstudio.watchrss.sdk.bili.BiliCookies
import com.lightningstudio.watchrss.sdk.bili.BiliFavoriteFolder
import com.lightningstudio.watchrss.sdk.bili.BiliFavoritePage
import com.lightningstudio.watchrss.sdk.bili.BiliFeedPage
import com.lightningstudio.watchrss.sdk.bili.BiliHistoryCursor
import com.lightningstudio.watchrss.sdk.bili.BiliHistoryPage
import com.lightningstudio.watchrss.sdk.bili.BiliPlayUrl
import com.lightningstudio.watchrss.sdk.bili.BiliResult
import com.lightningstudio.watchrss.sdk.bili.BiliSdkConfig
import com.lightningstudio.watchrss.sdk.bili.BiliToViewPage
import com.lightningstudio.watchrss.sdk.bili.BiliVideoDetail
import com.lightningstudio.watchrss.sdk.bili.EncryptedBiliAccountStore
import com.lightningstudio.watchrss.sdk.bili.QrPollResult
import com.lightningstudio.watchrss.sdk.bili.TvQrCode
import com.lightningstudio.watchrss.sdk.bili.WebQrCode

class BiliRepository(context: Context) {
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
}
