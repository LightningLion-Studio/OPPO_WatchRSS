package com.lightningstudio.watchrss.sdk.bili

data class BiliSdkConfig(
    val appKey: String = "1d8b6e7d45233436",
    val appSec: String = "560c52ccd288fed045859ed18bffd973",
    val tvAppKey: String = "4409e2ce8ffd12b8",
    val tvAppSec: String = "59b43e04ad6965f34319062b478f83dd",
    val tvMobiApp: String = "android_tv_yst",
    val tvLocalId: String = "0",
    val mobiApp: String = "android",
    val platform: String = "android",
    val build: Int = 7000000,
    val webUserAgent: String =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
    val appUserAgent: String =
        "Mozilla/5.0 (Linux; Android 12; OPPO WatchRSS) AppleWebKit/537.36 (KHTML, like Gecko) Mobile Safari/537.36",
    val webReferer: String = "https://www.bilibili.com/",
    val webBaseUrl: String = "https://api.bilibili.com",
    val appBaseUrl: String = "https://app.bilibili.com",
    val passportBaseUrl: String = "https://passport.bilibili.com"
)
