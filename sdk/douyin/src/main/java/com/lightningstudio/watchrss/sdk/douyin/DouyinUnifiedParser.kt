package com.lightningstudio.watchrss.sdk.douyin

import org.json.JSONException
import org.json.JSONObject

class DouyinUnifiedParser {
    fun parseFeed(jsonString: String): List<DouyinVideo> {
        val videoList: MutableList<DouyinVideo> = ArrayList()

        val root = JSONObject(jsonString)
        val awemeArray = root.getJSONArray("aweme_list")

        for (i in 0 until awemeArray.length()) {
            val item = awemeArray.getJSONObject(i)
            val video = DouyinVideo()

            video.awemeId = item.getString("aweme_id")
            video.desc = item.getString("desc")
            video.createTime = item.getLong("create_time")

            val author = item.getJSONObject("author")
            video.authorId = author.getString("uid")
            video.authorName = author.getString("nickname")
            video.authorAvatar = author.getJSONObject("avatar_thumb")
                .getJSONArray("url_list")
                .getString(0)

            val stats = item.getJSONObject("statistics")
            video.likeCount = stats.getLong("digg_count")
            video.commentCount = stats.getLong("comment_count")
            video.shareCount = stats.getLong("share_count")
            video.collectCount = stats.getLong("collect_count")

            val videoData = item.getJSONObject("video")
            video.duration = videoData.getInt("duration")
            video.playUrl = videoData.getJSONObject("play_addr")
                .getJSONArray("url_list")
                .getString(0)
            video.coverUrl = videoData.getJSONObject("cover")
                .getJSONArray("url_list")
                .getString(0)

            videoList.add(video)
        }

        return videoList
    }

    fun parse(jsonString: String): DouyinContent {
        val root = JSONObject(jsonString)

        val awemeDetailValue = root.opt("aweme_detail")

        if (awemeDetailValue == null || awemeDetailValue !is JSONObject) {
            throw JSONException("请求视频失败，如果您确定awemeID没问题，请查看原始响应JSON")
        }

        val awemeDetail = awemeDetailValue as JSONObject
        val awemeId = awemeDetail.getString("aweme_id")
        val desc = awemeDetail.getString("desc")
        val authorName = awemeDetail.getJSONObject("author").getString("nickname")
        val diggCount = awemeDetail.getJSONObject("statistics").getLong("digg_count")

        val type = awemeDetail.getInt("aweme_type")

        return if (type == 68 || type == 150) {
            val imagesArray = awemeDetail.getJSONArray("images")
            val urls = mutableListOf<String>()

            for (i in 0 until imagesArray.length()) {
                val imgObj = imagesArray.getJSONObject(i)
                val imgUrl = imgObj.getJSONArray("url_list").getString(0)
                urls.add(imgUrl)
            }

            if (urls.isEmpty()) throw JSONException("Note type but no images found")

            DouyinContent.Note(awemeId, desc, authorName, diggCount, urls)
        } else {
            val videoObj = awemeDetail.getJSONObject("video")
            val playAddr = videoObj.getJSONObject("play_addr")
            val playUrl = playAddr.getJSONArray("url_list").getString(0)
            val coverUrl = videoObj.getJSONObject("cover").getJSONArray("url_list").getString(0)

            DouyinContent.Video(awemeId, desc, authorName, diggCount, playUrl, coverUrl)
        }
    }
}
