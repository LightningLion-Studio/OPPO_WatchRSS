package com.lightningstudio.watchrss.sdk.bili

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class BiliIdentity(
    private val httpClient: BiliHttpClient,
    private val accountStore: BiliAccountStore?
) {
    suspend fun fetchBuvid(): BuvidResult? {
        val url = "https://api.bilibili.com/x/frontend/finger/spi"
        val response = httpClient.get(url, includeCookies = false)
        if (response.code != 200) return null
        val json = biliJson.parseToJsonElement(response.body).jsonObject
        val data = json["data"]?.jsonObject ?: return null
        val buvid3 = data["b_3"]?.jsonPrimitive?.content
        val buvid4 = data["b_4"]?.jsonPrimitive?.content
        if (buvid3.isNullOrBlank() && buvid4.isNullOrBlank()) return null
        accountStore?.update { current ->
            val updatedCookies = buildMap {
                if (!buvid3.isNullOrBlank()) put("buvid3", buvid3)
                if (!buvid4.isNullOrBlank()) put("buvid4", buvid4)
            }
            current.copy(
                cookies = if (updatedCookies.isEmpty()) current.cookies
                else BiliCookies.merge(current.cookies, updatedCookies),
                buvid3 = buvid3 ?: current.buvid3,
                buvid4 = buvid4 ?: current.buvid4,
                updatedAtMillis = System.currentTimeMillis()
            )
        }
        return BuvidResult(buvid3, buvid4)
    }

    suspend fun fetchWbiKeys(): WbiKeys? {
        val url = "https://api.bilibili.com/x/web-interface/nav"
        val response = httpClient.get(url)
        if (response.code != 200) return null
        val json = biliJson.parseToJsonElement(response.body).jsonObject
        val data = json["data"]?.jsonObject ?: return null
        val wbi = data["wbi_img"]?.jsonObject ?: return null
        val imgUrl = wbi["img_url"]?.jsonPrimitive?.content
        val subUrl = wbi["sub_url"]?.jsonPrimitive?.content
        val imgKey = BiliSigners.extractWbiKey(imgUrl)
        val subKey = BiliSigners.extractWbiKey(subUrl)
        if (imgKey.isNullOrBlank() || subKey.isNullOrBlank()) return null
        accountStore?.update { current ->
            current.copy(
                wbiImgKey = imgKey,
                wbiSubKey = subKey,
                updatedAtMillis = System.currentTimeMillis()
            )
        }
        return WbiKeys(imgKey, subKey)
    }

    suspend fun fetchWebTicket(csrf: String): String? {
        val url = "https://api.bilibili.com/bapis/bilibili.api.ticket.v1.Ticket/GenWebTicket"
        val response = httpClient.postForm(
            url,
            form = mapOf("csrf" to csrf),
            headers = mapOf("Referer" to "https://www.bilibili.com/")
        )
        if (response.code != 200) return null
        val json = biliJson.parseToJsonElement(response.body).jsonObject
        val data = json["data"]?.jsonObject ?: return null
        val ticket = data["ticket"]?.jsonPrimitive?.content
        if (ticket.isNullOrBlank()) return null
        accountStore?.update { current ->
            current.copy(biliTicket = ticket, updatedAtMillis = System.currentTimeMillis())
        }
        return ticket
    }

}

data class BuvidResult(
    val buvid3: String?,
    val buvid4: String?
)

data class WbiKeys(
    val imgKey: String,
    val subKey: String
)
