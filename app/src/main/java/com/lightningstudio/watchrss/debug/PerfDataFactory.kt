package com.lightningstudio.watchrss.debug

import com.lightningstudio.watchrss.data.rss.RssItem

object PerfDataFactory {
    private const val LARGE_LIST_COUNT = 10_000
    private const val LARGE_ARTICLE_TARGET_CHARS = 100_000

    fun buildLargeList(): List<RssItem> {
        val now = System.currentTimeMillis()
        return List(LARGE_LIST_COUNT) { index ->
            RssItem(
                id = index.toLong(),
                channelId = 0L,
                title = "性能测试条目 ${index + 1}",
                description = "这是用于滚动性能测试的摘要内容。",
                content = null,
                link = null,
                pubDate = null,
                imageUrl = null,
                audioUrl = null,
                videoUrl = null,
                summary = "这是用于滚动性能测试的摘要内容。",
                previewImageUrl = null,
                isRead = index % 3 == 0,
                isLiked = false,
                readingProgress = 0f,
                fetchedAt = now - index
            )
        }
    }

    fun buildLargeArticle(): RssItem {
        val content = buildLargeArticleContent()
        return RssItem(
            id = 0L,
            channelId = 0L,
            title = "性能测试：超长文章",
            description = null,
            content = content,
            link = null,
            pubDate = null,
            imageUrl = null,
            audioUrl = null,
            videoUrl = null,
            summary = "用于性能测试的超长正文。",
            previewImageUrl = null,
            isRead = false,
            isLiked = false,
            readingProgress = 0f,
            fetchedAt = System.currentTimeMillis()
        )
    }

    private fun buildLargeArticleContent(): String {
        val paragraph = "这是一段用于性能测试的文本内容，用来模拟超长正文。" +
            "为了压测排版与滚动性能，会重复生成多个段落。\n"
        val builder = StringBuilder()
        while (builder.length < LARGE_ARTICLE_TARGET_CHARS) {
            builder.append("<p>")
            builder.append(paragraph.repeat(4))
            builder.append("</p>")
        }
        return builder.toString()
    }
}
