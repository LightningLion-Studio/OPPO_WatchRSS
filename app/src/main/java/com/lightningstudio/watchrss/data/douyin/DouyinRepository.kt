package com.lightningstudio.watchrss.data.douyin

import android.content.Context
import com.lightningstudio.watchrss.sdk.douyin.ABogus
import com.lightningstudio.watchrss.sdk.douyin.DouyinContent
import com.lightningstudio.watchrss.sdk.douyin.DouyinUnifiedParser
import com.lightningstudio.watchrss.sdk.douyin.DouyinVideo
import com.lightningstudio.watchrss.sdk.douyin.DouyinWebCrawler
import com.lightningstudio.watchrss.sdk.douyin.EncryptedDouyinCookieStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.io.IOException

class DouyinRepository(context: Context) {
    private val cookieStore = EncryptedDouyinCookieStore(context)
    private val parser = DouyinUnifiedParser()
    private val crawler = DouyinWebCrawler(ABogus())

    suspend fun isLoggedIn(): Boolean = !cookieStore.readCookie().isNullOrBlank()

    suspend fun readCookie(): String? = cookieStore.readCookie()

    suspend fun clearCookie() {
        cookieStore.writeCookie(null)
    }

    suspend fun applyCookieHeader(rawCookie: String): Result<Unit> {
        val trimmed = rawCookie.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("缺少有效 Cookie"))
        }
        cookieStore.writeCookie(trimmed)
        return Result.success(Unit)
    }

    suspend fun fetchFeed(): DouyinResult<List<DouyinVideo>> {
        val cookie = cookieStore.readCookie().orEmpty()
        if (cookie.isBlank()) {
            return DouyinResult(DouyinErrorCodes.NOT_LOGGED_IN, "未登录")
        }
        return withContext(Dispatchers.IO) {
            try {
                val raw = crawler.fetchJingxuanFeed(cookie)
                val list = parser.parseFeed(raw)
                DouyinResult(DouyinErrorCodes.OK, data = list)
            } catch (e: IOException) {
                val msg = e.message.orEmpty()
                val code = if (msg.contains("Cookie无效")) {
                    DouyinErrorCodes.NOT_LOGGED_IN
                } else {
                    DouyinErrorCodes.REQUEST_FAILED
                }
                DouyinResult(code, msg.ifBlank { "网络请求失败" })
            } catch (e: JSONException) {
                DouyinResult(DouyinErrorCodes.PARSE_FAILED, e.message ?: "解析失败")
            }
        }
    }

    suspend fun fetchVideo(awemeId: String): DouyinResult<DouyinContent> {
        val cookie = cookieStore.readCookie().orEmpty()
        if (cookie.isBlank()) {
            return DouyinResult(DouyinErrorCodes.NOT_LOGGED_IN, "未登录")
        }
        return withContext(Dispatchers.IO) {
            try {
                val raw = crawler.fetchOneVideo(awemeId, cookie)
                val content = parser.parse(raw)
                DouyinResult(DouyinErrorCodes.OK, data = content)
            } catch (e: IOException) {
                val msg = e.message.orEmpty()
                val code = if (msg.contains("Cookie无效")) {
                    DouyinErrorCodes.NOT_LOGGED_IN
                } else {
                    DouyinErrorCodes.REQUEST_FAILED
                }
                DouyinResult(code, msg.ifBlank { "网络请求失败" })
            } catch (e: JSONException) {
                DouyinResult(DouyinErrorCodes.PARSE_FAILED, e.message ?: "解析失败")
            }
        }
    }

    suspend fun buildPlayHeaders(): Map<String, String> {
        val cookie = cookieStore.readCookie()
        val headers = mutableMapOf(
            "User-Agent" to DouyinWebCrawler.USER_AGENT,
            "Referer" to DouyinWebCrawler.REFERER
        )
        if (!cookie.isNullOrBlank()) {
            headers["Cookie"] = cookie
        }
        return headers
    }
}

object DouyinErrorCodes {
    const val OK = 0
    const val NOT_LOGGED_IN = 401
    const val REQUEST_FAILED = 500
    const val PARSE_FAILED = 501
}

data class DouyinResult<T>(
    val code: Int,
    val message: String? = null,
    val data: T? = null
) {
    val isSuccess: Boolean = code == DouyinErrorCodes.OK
}

fun formatDouyinError(code: Int, message: String? = null): String {
    return when (code) {
        DouyinErrorCodes.NOT_LOGGED_IN -> "需要登录"
        DouyinErrorCodes.PARSE_FAILED -> "解析失败"
        DouyinErrorCodes.REQUEST_FAILED -> "网络请求失败"
        else -> message?.takeIf { it.isNotBlank() } ?: "加载失败"
    }
}
