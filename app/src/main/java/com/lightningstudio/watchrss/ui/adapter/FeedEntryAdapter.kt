package com.lightningstudio.watchrss.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.heytap.wearable.support.recycler.widget.RecyclerView
import com.heytap.wearable.support.widget.HeyButton
import com.heytap.wearable.support.widget.HeyTextView
import com.lightningstudio.watchrss.R
import com.lightningstudio.watchrss.data.rss.RssItem
import com.lightningstudio.watchrss.data.rss.RssMediaExtractor
import com.lightningstudio.watchrss.ui.util.formatRssSummary
import com.lightningstudio.watchrss.ui.util.RssImageLoader
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope

sealed class FeedEntry {
    data class Header(val title: String, val isRefreshing: Boolean) : FeedEntry()
    data class Item(val item: RssItem) : FeedEntry()
    data object Empty : FeedEntry()
    data class Actions(val canLoadMore: Boolean, val isRefreshing: Boolean) : FeedEntry()
}

class FeedEntryAdapter(
    private val onItemClick: (RssItem) -> Unit,
    private val onItemLongClick: (RssItem) -> Unit,
    private val onHeaderClick: () -> Unit,
    private val onRefreshClick: () -> Unit,
    private val onLoadMoreClick: () -> Unit,
    private val onBackClick: () -> Unit
) : RecyclerView.Adapter<FeedEntryAdapter.EntryViewHolder>() {
    private val entries = mutableListOf<FeedEntry>()
    private val thumbUrlCache = mutableMapOf<Long, String?>()

    fun getEntry(position: Int): FeedEntry? = entries.getOrNull(position)

    fun submit(title: String, items: List<RssItem>, isRefreshing: Boolean, hasMore: Boolean) {
        entries.clear()
        thumbUrlCache.clear()
        entries.add(FeedEntry.Header(title, isRefreshing))
        if (items.isEmpty()) {
            entries.add(FeedEntry.Empty)
        } else {
            entries.addAll(items.map { FeedEntry.Item(it) })
            entries.add(FeedEntry.Actions(hasMore, isRefreshing))
        }
        notifyDataSetChanged()
    }

    fun updateTitle(title: String) {
        val headerIndex = entries.indexOfFirst { it is FeedEntry.Header }
        if (headerIndex >= 0) {
            val refreshing = (entries[headerIndex] as? FeedEntry.Header)?.isRefreshing ?: false
            entries[headerIndex] = FeedEntry.Header(title, refreshing)
            notifyItemChanged(headerIndex)
        }
    }

    fun updateRefreshing(isRefreshing: Boolean) {
        val headerIndex = entries.indexOfFirst { it is FeedEntry.Header }
        if (headerIndex >= 0) {
            val title = (entries[headerIndex] as? FeedEntry.Header)?.title ?: "RSS"
            entries[headerIndex] = FeedEntry.Header(title, isRefreshing)
            notifyItemChanged(headerIndex)
        }
        updateActions { actions -> actions.copy(isRefreshing = isRefreshing) }
    }

    fun updateHasMore(canLoadMore: Boolean) {
        updateActions { actions -> actions.copy(canLoadMore = canLoadMore) }
    }

    private fun updateActions(update: (FeedEntry.Actions) -> FeedEntry.Actions) {
        val actionIndex = entries.indexOfFirst { it is FeedEntry.Actions }
        if (actionIndex >= 0) {
            val current = entries[actionIndex] as? FeedEntry.Actions ?: return
            entries[actionIndex] = update(current)
            notifyItemChanged(actionIndex)
        }
    }

    override fun getItemViewType(position: Int): Int = when (val entry = entries[position]) {
        is FeedEntry.Header -> TYPE_HEADER
        is FeedEntry.Empty -> TYPE_EMPTY
        is FeedEntry.Actions -> TYPE_ACTIONS
        is FeedEntry.Item -> if (hasThumb(entry.item)) TYPE_ITEM_IMAGE else TYPE_ITEM_TEXT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> {
                val view = inflater.inflate(R.layout.item_feed_header, parent, false)
                EntryViewHolder.HeaderViewHolder(view, onHeaderClick)
            }
            TYPE_EMPTY -> {
                val view = inflater.inflate(R.layout.item_feed_empty, parent, false)
                EntryViewHolder.EmptyViewHolder(view, onRefreshClick, onBackClick)
            }
            TYPE_ACTIONS -> {
                val view = inflater.inflate(R.layout.item_feed_actions, parent, false)
                EntryViewHolder.ActionsViewHolder(view, onRefreshClick, onLoadMoreClick)
            }
            TYPE_ITEM_TEXT -> {
                val view = inflater.inflate(R.layout.item_feed_text_card, parent, false)
                EntryViewHolder.TextItemViewHolder(view, onItemClick, onItemLongClick)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_feed_card, parent, false)
                EntryViewHolder.ImageItemViewHolder(
                    view,
                    onItemClick,
                    onItemLongClick,
                    resolveThumbUrl = { item -> resolveThumbUrl(item) }
                )
            }
        }
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        holder.bind(entries[position])
        if (holder is EntryViewHolder.ImageItemViewHolder) {
            preloadNearbyImages(position, holder.itemView)
        }
    }

    override fun getItemCount(): Int = entries.size

    sealed class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(entry: FeedEntry)

        class HeaderViewHolder(
            itemView: View,
            private val onHeaderClick: () -> Unit
        ) : EntryViewHolder(itemView) {
            private val titleView: HeyTextView = itemView.findViewById(R.id.text_feed_title)
            private val hintView: HeyTextView = itemView.findViewById(R.id.text_feed_hint)

            override fun bind(entry: FeedEntry) {
                if (entry is FeedEntry.Header) {
                    titleView.text = entry.title
                    hintView.text = if (entry.isRefreshing) "正在刷新中..." else "下拉刷新"
                    itemView.setOnClickListener { onHeaderClick() }
                }
            }
        }

        class EmptyViewHolder(
            itemView: View,
            private val onRefreshClick: () -> Unit,
            private val onBackClick: () -> Unit
        ) : EntryViewHolder(itemView) {
            private val refreshButton: HeyButton = itemView.findViewById(R.id.button_empty_refresh)
            private val backButton: HeyButton = itemView.findViewById(R.id.button_empty_back)

            override fun bind(entry: FeedEntry) {
                refreshButton.setOnClickListener { onRefreshClick() }
                backButton.setOnClickListener { onBackClick() }
            }
        }

        class ActionsViewHolder(
            itemView: View,
            private val onRefreshClick: () -> Unit,
            private val onLoadMoreClick: () -> Unit
        ) : EntryViewHolder(itemView) {
            private val refreshButton: HeyButton = itemView.findViewById(R.id.button_refresh)
            private val loadMoreButton: HeyButton = itemView.findViewById(R.id.button_load_more)

            override fun bind(entry: FeedEntry) {
                if (entry is FeedEntry.Actions) {
                    refreshButton.isEnabled = !entry.isRefreshing
                    refreshButton.text = if (entry.isRefreshing) "刷新中" else "刷新"
                    loadMoreButton.isEnabled = entry.canLoadMore
                    loadMoreButton.alpha = if (entry.canLoadMore) 1f else 0.5f
                    loadMoreButton.text = if (entry.canLoadMore) "加载更多" else "没有更多"

                    refreshButton.setOnClickListener { onRefreshClick() }
                    loadMoreButton.setOnClickListener { onLoadMoreClick() }
                }
            }
        }

        class ImageItemViewHolder(
            itemView: View,
            private val onItemClick: (RssItem) -> Unit,
            private val onItemLongClick: (RssItem) -> Unit,
            private val resolveThumbUrl: (RssItem) -> String?
        ) : EntryViewHolder(itemView) {
            private val titleView: HeyTextView = itemView.findViewById(R.id.text_item_title)
            private val summaryView: HeyTextView = itemView.findViewById(R.id.text_item_summary)
            private val thumbView: android.widget.ImageView = itemView.findViewById(R.id.image_thumb)
            private val unreadView: android.widget.ImageView = itemView.findViewById(R.id.image_unread)

            override fun bind(entry: FeedEntry) {
                if (entry !is FeedEntry.Item) return
                val item = entry.item
                resetViewState(itemView)
                titleView.text = item.title
                val summary = formatRssSummary(item.description) ?: "暂无摘要"
                summaryView.text = summary

                unreadView.visibility = if (item.isRead) View.GONE else View.VISIBLE

                val thumbUrl = resolveThumbUrl(item)

                if (!thumbUrl.isNullOrBlank()) {
                    val scope = itemView.findViewTreeLifecycleOwner()?.lifecycleScope
                    if (scope != null) {
                        val maxWidth = itemView.resources.displayMetrics.widthPixels
                        RssImageLoader.load(itemView.context, thumbUrl, thumbView, scope, maxWidth)
                    }
                } else {
                    thumbView.setImageDrawable(null)
                    thumbView.setBackgroundColor(0xFF1C1C1C.toInt())
                }

                itemView.setOnClickListener { onItemClick(item) }
                itemView.setOnLongClickListener {
                    onItemLongClick(item)
                    true
                }
            }

            private fun resetViewState(view: View) {
                view.isPressed = false
                view.isSelected = false
                view.isActivated = false
                view.scaleX = 1f
                view.scaleY = 1f
                view.alpha = 1f
            }
        }

        class TextItemViewHolder(
            itemView: View,
            private val onItemClick: (RssItem) -> Unit,
            private val onItemLongClick: (RssItem) -> Unit
        ) : EntryViewHolder(itemView) {
            private val titleView: HeyTextView = itemView.findViewById(R.id.text_item_title)
            private val summaryView: HeyTextView = itemView.findViewById(R.id.text_item_summary)
            private val unreadView: android.widget.ImageView = itemView.findViewById(R.id.image_unread)

            override fun bind(entry: FeedEntry) {
                if (entry !is FeedEntry.Item) return
                val item = entry.item
                titleView.text = item.title
                val summary = formatRssSummary(item.description) ?: "暂无摘要"
                summaryView.text = summary
                unreadView.visibility = if (item.isRead) View.GONE else View.VISIBLE
                itemView.setOnClickListener { onItemClick(item) }
                itemView.setOnLongClickListener {
                    onItemLongClick(item)
                    true
                }
            }
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM_IMAGE = 1
        private const val TYPE_EMPTY = 2
        private const val TYPE_ACTIONS = 3
        private const val TYPE_ITEM_TEXT = 4
        private const val PRELOAD_COUNT = 3
    }

    private fun hasThumb(item: RssItem): Boolean = resolveThumbUrl(item) != null

    private fun resolveThumbUrl(item: RssItem): String? {
        return thumbUrlCache.getOrPut(item.id) {
            val candidate = item.imageUrl?.takeIf { it.isNotBlank() }
                ?: firstImageFromHtml(item.content)
                ?: firstImageFromHtml(item.description)
            normalizeMediaUrl(candidate, item.link)
        }
    }

    private fun firstImageFromHtml(html: String?): String? {
        if (html.isNullOrBlank()) return null
        val image = RssMediaExtractor.extractFromHtml(html)
            .firstOrNull { it.type == com.lightningstudio.watchrss.data.rss.OfflineMediaType.IMAGE }
        return image?.url
    }

    private fun normalizeMediaUrl(raw: String?, baseLink: String?): String? {
        val trimmed = raw?.trim()?.ifEmpty { return null } ?: return null
        if (trimmed.startsWith("data:", ignoreCase = true)) return null
        val sanitized = trimmed.replace(" ", "%20")
        if (sanitized.startsWith("//")) {
            val scheme = baseLink?.let { kotlin.runCatching { java.net.URI(it).scheme }.getOrNull() }
                ?: "https"
            return "$scheme:$sanitized"
        }
        if (sanitized.startsWith("http://") || sanitized.startsWith("https://") ||
            sanitized.startsWith("file://") || sanitized.startsWith("/")
        ) {
            if (sanitized.startsWith("/") && !baseLink.isNullOrBlank()) {
                return resolveRelativeUrl(baseLink, sanitized)
            }
            if (sanitized.startsWith("/") && baseLink.isNullOrBlank()) {
                return if (java.io.File(sanitized).exists()) sanitized else null
            }
            return sanitized
        }
        if (!sanitized.contains("://")) {
            if (sanitized.startsWith("www.", ignoreCase = true)) {
                return "https://$sanitized"
            }
            if (!baseLink.isNullOrBlank()) {
                return resolveRelativeUrl(baseLink, sanitized)
            }
        }
        return null
    }

    private fun resolveRelativeUrl(baseLink: String, relative: String): String? {
        return try {
            java.net.URL(java.net.URL(baseLink), relative).toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun preloadNearbyImages(position: Int, itemView: View) {
        val scope = itemView.findViewTreeLifecycleOwner()?.lifecycleScope ?: return
        val context = itemView.context
        val maxWidth = itemView.resources.displayMetrics.widthPixels
        val end = (position + PRELOAD_COUNT).coerceAtMost(entries.lastIndex)
        for (index in (position + 1)..end) {
            val entry = entries.getOrNull(index) as? FeedEntry.Item ?: continue
            val url = resolveThumbUrl(entry.item) ?: continue
            RssImageLoader.preload(context, url, scope, maxWidth)
        }
    }

}
