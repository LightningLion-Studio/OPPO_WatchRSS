package com.lightningstudio.watchrss.ui.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import com.heytap.wearable.support.recycler.widget.RecyclerView
import com.heytap.wearable.support.widget.HeyButton
import com.heytap.wearable.support.widget.HeyTextView
import com.lightningstudio.watchrss.R
import com.lightningstudio.watchrss.data.rss.RssItem
import com.lightningstudio.watchrss.data.rss.RssMediaExtractor
import com.lightningstudio.watchrss.ui.util.formatRssSummary
import com.lightningstudio.watchrss.ui.util.RssImageLoader
import kotlin.math.abs

sealed class FeedEntry {
    data class Header(val title: String, val isRefreshing: Boolean) : FeedEntry()
    data class Item(val item: RssItem) : FeedEntry()
    data object Empty : FeedEntry()
    data class Actions(val canLoadMore: Boolean, val isRefreshing: Boolean) : FeedEntry()
}

class FeedEntryAdapter(
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val onItemClick: (RssItem) -> Unit,
    private val onItemLongClick: (RssItem) -> Unit,
    private val onFavoriteClick: (RssItem) -> Unit,
    private val onWatchLaterClick: (RssItem) -> Unit,
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
                EntryViewHolder.TextItemViewHolder(
                    view,
                    onItemClick,
                    onItemLongClick,
                    onFavoriteClick,
                    onWatchLaterClick
                )
            }
            else -> {
                val view = inflater.inflate(R.layout.item_feed_card, parent, false)
                EntryViewHolder.ImageItemViewHolder(
                    view,
                    onItemClick,
                    onItemLongClick,
                    onFavoriteClick,
                    onWatchLaterClick,
                    scope,
                    resolveThumbUrl = { item -> resolveThumbUrl(item) }
                )
            }
        }
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        holder.bind(entries[position])
        if (holder is EntryViewHolder.ImageItemViewHolder) {
            preloadNearbyImages(position, holder.itemView, scope)
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
            private val onFavoriteClick: (RssItem) -> Unit,
            private val onWatchLaterClick: (RssItem) -> Unit,
            private val scope: kotlinx.coroutines.CoroutineScope,
            private val resolveThumbUrl: (RssItem) -> String?
        ) : EntryViewHolder(itemView) {
            private val titleView: HeyTextView = itemView.findViewById(R.id.text_item_title)
            private val summaryView: HeyTextView = itemView.findViewById(R.id.text_item_summary)
            private val thumbView: android.widget.ImageView = itemView.findViewById(R.id.image_thumb)
            private val unreadView: android.widget.ImageView = itemView.findViewById(R.id.image_unread)
            private val favoriteAction: HeyTextView = itemView.findViewById(R.id.action_favorite)
            private val watchLaterAction: HeyTextView = itemView.findViewById(R.id.action_watch_later)
            private val swipeActions: View? = itemView.findViewById(R.id.swipe_actions)
            private val swipeCover: View? = itemView.findViewById(R.id.swipe_cover)
            private val swipeContent: View? = itemView.findViewById(R.id.swipe_content)
            private val clickTarget: View = swipeContent ?: itemView
            private val pressScaleListener = PressScaleListener(clickTarget, swipeCover)

            init {
                clickTarget.setTag(R.id.tag_skip_scale_reset, true)
                clickTarget.setTag(R.id.tag_skip_translation_reset, true)
                swipeCover?.setTag(R.id.tag_skip_scale_reset, true)
                swipeCover?.setTag(R.id.tag_skip_translation_reset, true)
                clickTarget.setOnTouchListener(pressScaleListener)
            }

            override fun bind(entry: FeedEntry) {
                if (entry !is FeedEntry.Item) return
                val item = entry.item
                resetViewState(itemView)
                resetViewState(clickTarget)
                swipeCover?.let { resetViewState(it) }
                resetSwipeState()
                titleView.text = item.title
                val summary = formatRssSummary(item.description) ?: "暂无摘要"
                summaryView.text = summary

                unreadView.visibility = if (item.isRead) View.GONE else View.VISIBLE

                val thumbUrl = resolveThumbUrl(item)

                if (!thumbUrl.isNullOrBlank()) {
                    val maxWidth = itemView.resources.displayMetrics.widthPixels
                    RssImageLoader.load(itemView.context, thumbUrl, thumbView, scope, maxWidth)
                } else {
                    thumbView.setImageDrawable(null)
                    thumbView.setBackgroundColor(0xFF1C1C1C.toInt())
                }

                clickTarget.setOnClickListener { onItemClick(item) }
                clickTarget.setOnLongClickListener {
                    onItemLongClick(item)
                    true
                }
                favoriteAction.setOnClickListener { onFavoriteClick(item) }
                watchLaterAction.setOnClickListener { onWatchLaterClick(item) }
                syncActionHeight()
            }

            private fun resetViewState(view: View) {
                view.isPressed = false
                view.isSelected = false
                view.isActivated = false
                view.scaleX = 1f
                view.scaleY = 1f
                view.alpha = 1f
            }

            private fun resetSwipeState() {
                swipeContent?.translationX = 0f
                swipeCover?.translationX = 0f
            }

            private fun syncActionHeight() {
                val actions = swipeActions ?: return
                val cover = swipeCover
                val content = swipeContent ?: return
                if (content.height > 0) {
                    if (actions.layoutParams.height != content.height) {
                        actions.layoutParams = actions.layoutParams.apply { this.height = content.height }
                    }
                    if (cover != null && cover.layoutParams.height != content.height) {
                        cover.layoutParams = cover.layoutParams.apply { this.height = content.height }
                    }
                } else {
                    content.post {
                        val contentHeight = content.height
                        if (contentHeight > 0 && actions.layoutParams.height != contentHeight) {
                            actions.layoutParams = actions.layoutParams.apply { this.height = contentHeight }
                        }
                        if (cover != null && contentHeight > 0 && cover.layoutParams.height != contentHeight) {
                            cover.layoutParams = cover.layoutParams.apply { this.height = contentHeight }
                        }
                    }
                }
            }
        }

        class TextItemViewHolder(
            itemView: View,
            private val onItemClick: (RssItem) -> Unit,
            private val onItemLongClick: (RssItem) -> Unit,
            private val onFavoriteClick: (RssItem) -> Unit,
            private val onWatchLaterClick: (RssItem) -> Unit
        ) : EntryViewHolder(itemView) {
            private val titleView: HeyTextView = itemView.findViewById(R.id.text_item_title)
            private val summaryView: HeyTextView = itemView.findViewById(R.id.text_item_summary)
            private val unreadView: android.widget.ImageView = itemView.findViewById(R.id.image_unread)
            private val favoriteAction: HeyTextView = itemView.findViewById(R.id.action_favorite)
            private val watchLaterAction: HeyTextView = itemView.findViewById(R.id.action_watch_later)
            private val swipeActions: View? = itemView.findViewById(R.id.swipe_actions)
            private val swipeCover: View? = itemView.findViewById(R.id.swipe_cover)
            private val swipeContent: View? = itemView.findViewById(R.id.swipe_content)
            private val clickTarget: View = swipeContent ?: itemView
            private val pressScaleListener = PressScaleListener(clickTarget, swipeCover)

            init {
                clickTarget.setTag(R.id.tag_skip_scale_reset, true)
                clickTarget.setTag(R.id.tag_skip_translation_reset, true)
                swipeCover?.setTag(R.id.tag_skip_scale_reset, true)
                swipeCover?.setTag(R.id.tag_skip_translation_reset, true)
                clickTarget.setOnTouchListener(pressScaleListener)
            }

            override fun bind(entry: FeedEntry) {
                if (entry !is FeedEntry.Item) return
                val item = entry.item
                resetViewState(clickTarget)
                swipeCover?.let { resetViewState(it) }
                resetSwipeState()
                titleView.text = item.title
                val summary = formatRssSummary(item.description) ?: "暂无摘要"
                summaryView.text = summary
                unreadView.visibility = if (item.isRead) View.GONE else View.VISIBLE
                clickTarget.setOnClickListener { onItemClick(item) }
                clickTarget.setOnLongClickListener {
                    onItemLongClick(item)
                    true
                }
                favoriteAction.setOnClickListener { onFavoriteClick(item) }
                watchLaterAction.setOnClickListener { onWatchLaterClick(item) }
                syncActionHeight()
            }

            private fun resetViewState(view: View) {
                view.isPressed = false
                view.isSelected = false
                view.isActivated = false
                view.scaleX = 1f
                view.scaleY = 1f
                view.alpha = 1f
            }

            private fun resetSwipeState() {
                swipeContent?.translationX = 0f
                swipeCover?.translationX = 0f
            }

            private fun syncActionHeight() {
                val actions = swipeActions ?: return
                val cover = swipeCover
                val content = swipeContent ?: return
                if (content.height > 0) {
                    if (actions.layoutParams.height != content.height) {
                        actions.layoutParams = actions.layoutParams.apply { this.height = content.height }
                    }
                    if (cover != null && cover.layoutParams.height != content.height) {
                        cover.layoutParams = cover.layoutParams.apply { this.height = content.height }
                    }
                } else {
                    content.post {
                        val contentHeight = content.height
                        if (contentHeight > 0 && actions.layoutParams.height != contentHeight) {
                            actions.layoutParams = actions.layoutParams.apply { this.height = contentHeight }
                        }
                        if (cover != null && contentHeight > 0 && cover.layoutParams.height != contentHeight) {
                            cover.layoutParams = cover.layoutParams.apply { this.height = contentHeight }
                        }
                    }
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
        private const val PRESS_SCALE = 0.97f
        private const val PRESS_DOWN_DURATION_MS = 240L
        private const val PRESS_UP_DURATION_MS = 360L
        private const val PRESS_HOLD_MS = 300L
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

    private fun preloadNearbyImages(position: Int, itemView: View, scope: kotlinx.coroutines.CoroutineScope) {
        val context = itemView.context
        val maxWidth = itemView.resources.displayMetrics.widthPixels
        val end = (position + PRELOAD_COUNT).coerceAtMost(entries.lastIndex)
        for (index in (position + 1)..end) {
            val entry = entries.getOrNull(index) as? FeedEntry.Item ?: continue
            val url = resolveThumbUrl(entry.item) ?: continue
            RssImageLoader.preload(context, url, scope, maxWidth)
        }
    }

    private class PressScaleListener(
        private val target: View,
        private val extra: View? = null
    ) : View.OnTouchListener {
        private val slop = ViewConfiguration.get(target.context).scaledTouchSlop
        private val scaleTargets = if (extra != null && extra !== target) {
            listOf(target, extra)
        } else {
            listOf(target)
        }
        private var downX = 0f
        private var downY = 0f
        private var clickCandidate = false
        private var releaseHandled = false
        private var holdRunnable: Runnable? = null
        private var upRunnable: Runnable? = null

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    clickCandidate = true
                    releaseHandled = false
                    clearCallbacks()
                    startDownAnimation()
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!releaseHandled && clickCandidate && movedOutOfSlop(event)) {
                        clickCandidate = false
                        releaseHandled = true
                        cancelToUp()
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    if (releaseHandled) return false
                    clickCandidate = false
                    releaseHandled = true
                    cancelToUp()
                }
                MotionEvent.ACTION_UP -> {
                    if (releaseHandled) return false
                    releaseHandled = true
                    if (clickCandidate) {
                        scheduleClickHold()
                    } else {
                        cancelToUp()
                    }
                }
            }
            return false
        }

        private fun movedOutOfSlop(event: MotionEvent): Boolean {
            val dx = abs(event.x - downX)
            val dy = abs(event.y - downY)
            return dx > slop || dy > slop
        }

        private fun startDownAnimation() {
            scaleTargets.forEach { view ->
                view.animate().cancel()
                view.animate()
                    .scaleX(FeedEntryAdapter.PRESS_SCALE)
                    .scaleY(FeedEntryAdapter.PRESS_SCALE)
                    .setDuration(FeedEntryAdapter.PRESS_DOWN_DURATION_MS)
                    .start()
            }
        }

        private fun scheduleClickHold() {
            clearCallbacks()
            holdRunnable = Runnable {
                setScaleInstant(FeedEntryAdapter.PRESS_SCALE)
                upRunnable = Runnable { animateScaleUp() }
                target.postDelayed(upRunnable, FeedEntryAdapter.PRESS_HOLD_MS)
            }
            target.post(holdRunnable)
        }

        private fun cancelToUp() {
            clearCallbacks()
            animateScaleUp()
        }

        private fun clearCallbacks() {
            holdRunnable?.let { target.removeCallbacks(it) }
            holdRunnable = null
            upRunnable?.let { target.removeCallbacks(it) }
            upRunnable = null
        }

        private fun animateScaleUp() {
            scaleTargets.forEach { view ->
                view.animate().cancel()
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(FeedEntryAdapter.PRESS_UP_DURATION_MS)
                    .start()
            }
        }

        private fun setScaleInstant(scale: Float) {
            scaleTargets.forEach { view ->
                view.scaleX = scale
                view.scaleY = scale
            }
        }
    }

}
