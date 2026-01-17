package com.lightningstudio.watchrss.ui.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import com.heytap.wearable.support.recycler.widget.RecyclerView
import com.heytap.wearable.support.widget.HeyMultipleDefaultItem
import com.lightningstudio.watchrss.R
import com.lightningstudio.watchrss.data.rss.RssChannel
import com.lightningstudio.watchrss.ui.util.formatTime
import kotlin.math.abs

sealed class HomeEntry {
    data object Profile : HomeEntry()
    data class Channel(val channel: RssChannel) : HomeEntry()
    data object Empty : HomeEntry()
    data object AddRss : HomeEntry()
}

class HomeEntryAdapter(
    private val onProfileClick: () -> Unit,
    private val onChannelClick: (RssChannel) -> Unit,
    private val onChannelLongClick: (RssChannel) -> Unit,
    private val onAddRssClick: () -> Unit,
    private val onMoveTopClick: (RssChannel) -> Unit,
    private val onMarkReadClick: (RssChannel) -> Unit
) : RecyclerView.Adapter<HomeEntryAdapter.EntryViewHolder>() {
    private val items = mutableListOf<HomeEntry>()

    fun submitList(channels: List<RssChannel>) {
        items.clear()
        items.add(HomeEntry.Profile)
        if (channels.isEmpty()) {
            items.add(HomeEntry.Empty)
        } else {
            items.addAll(channels.map { HomeEntry.Channel(it) })
        }
        items.add(HomeEntry.AddRss)
        notifyDataSetChanged()
    }

    fun getEntry(position: Int): HomeEntry? = items.getOrNull(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        return when (viewType) {
            TYPE_PROFILE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_home_profile, parent, false)
                EntryViewHolder.ProfileViewHolder(view, onProfileClick)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_home_entry, parent, false)
                EntryViewHolder.DefaultViewHolder(
                    view,
                    onChannelClick,
                    onChannelLongClick,
                    onAddRssClick,
                    onMoveTopClick,
                    onMarkReadClick
                )
            }
        }
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        HomeEntry.Profile -> TYPE_PROFILE
        else -> TYPE_DEFAULT
    }

    sealed class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(entry: HomeEntry)

        class ProfileViewHolder(
            itemView: View,
            private val onProfileClick: () -> Unit
        ) : EntryViewHolder(itemView) {
            private val avatar = itemView.findViewById<com.heytap.wearable.support.widget.HeyTextView>(
                R.id.text_avatar
            )
            private val name = itemView.findViewById<com.heytap.wearable.support.widget.HeyTextView>(
                R.id.text_profile_name
            )
            private val hint = itemView.findViewById<com.heytap.wearable.support.widget.HeyTextView>(
                R.id.text_profile_hint
            )

            override fun bind(entry: HomeEntry) {
                if (entry !is HomeEntry.Profile) return
                avatar.text = "我"
                name.text = "未登录"
                hint.text = "点击进入我的"
                itemView.setOnClickListener { onProfileClick() }
            }
        }

        class DefaultViewHolder(
            itemView: View,
            private val onChannelClick: (RssChannel) -> Unit,
            private val onChannelLongClick: (RssChannel) -> Unit,
            private val onAddRssClick: () -> Unit,
            private val onMoveTopClick: (RssChannel) -> Unit,
            private val onMarkReadClick: (RssChannel) -> Unit
        ) : EntryViewHolder(itemView) {
            private val contentView: HeyMultipleDefaultItem =
                itemView.findViewById(R.id.home_entry_item)
            private val swipeActions: View? = itemView.findViewById(R.id.swipe_actions)
            private val swipeCover: View? = itemView.findViewById(R.id.swipe_cover)
            private val swipeContent: View? = itemView.findViewById(R.id.swipe_content)
            private val moveTopAction: com.heytap.wearable.support.widget.HeyTextView? =
                itemView.findViewById(R.id.action_move_top)
            private val markReadAction: com.heytap.wearable.support.widget.HeyTextView? =
                itemView.findViewById(R.id.action_mark_read)
            private val pressScaleListener = PressScaleListener(contentView, swipeCover)

            init {
                contentView.setTag(R.id.tag_skip_scale_reset, true)
                swipeCover?.setTag(R.id.tag_skip_scale_reset, true)
                swipeCover?.setTag(R.id.tag_skip_translation_reset, true)
                swipeContent?.setTag(R.id.tag_skip_translation_reset, true)
                contentView.setOnTouchListener(pressScaleListener)
            }

            override fun bind(entry: HomeEntry) {
                resetViewState()
                swipeCover?.let { resetViewState(it) }
                val indicator = contentView.getMinorImageView()
                indicator?.visibility = View.GONE
                contentView.setOnLongClickListener(null)
                swipeActions?.visibility = View.GONE
                swipeCover?.visibility = View.GONE
                resetSwipeState()

                when (entry) {
                    is HomeEntry.Channel -> bindChannel(entry.channel, indicator)
                    HomeEntry.Empty -> bindEmpty()
                    HomeEntry.AddRss -> bindAddRss()
                    HomeEntry.Profile -> Unit
                }

                syncActionHeight()
            }

            private fun bindChannel(channel: RssChannel, indicator: android.widget.ImageView?) {
                swipeActions?.visibility = View.VISIBLE
                swipeCover?.visibility = View.VISIBLE
                contentView.setTitle(channel.title)
                val summary = channel.description?.takeIf { it.isNotBlank() } ?: channel.url
                val pinLabel = if (channel.isPinned) "置顶 · " else ""
                val unreadLabel = if (channel.unreadCount > 0) "未读 ${channel.unreadCount} · " else ""
                val timeText = formatTime(channel.lastFetchedAt)
                contentView.setSummary("$pinLabel$summary\n${unreadLabel}更新: $timeText")

                if (channel.unreadCount > 0) {
                    indicator?.visibility = View.VISIBLE
                    indicator?.setImageResource(R.drawable.rss_unread_dot)
                }

                contentView.setBackgroundResource(
                    if (channel.isPinned) {
                        R.drawable.bg_settings_card_pinned
                    } else {
                        R.drawable.bg_settings_card
                    }
                )

                contentView.setOnClickListener { onChannelClick(channel) }
                contentView.setOnLongClickListener {
                    onChannelLongClick(channel)
                    true
                }

                moveTopAction?.setOnClickListener { onMoveTopClick(channel) }

                val isBuiltin = com.lightningstudio.watchrss.data.rss.BuiltinChannelType
                    .fromUrl(channel.url) != null
                val canMarkRead = channel.unreadCount > 0 && !isBuiltin
                markReadAction?.isEnabled = canMarkRead
                markReadAction?.alpha = if (canMarkRead) 1f else 0.5f
                markReadAction?.setOnClickListener(if (canMarkRead) {
                    { onMarkReadClick(channel) }
                } else {
                    null
                })
            }

            private fun bindEmpty() {
                contentView.setBackgroundResource(R.drawable.bg_settings_card)
                contentView.setTitle("还没有 RSS 频道")
                contentView.setSummary("点击下方添加你的第一个订阅源")
                contentView.setOnClickListener(null)
            }

            private fun bindAddRss() {
                contentView.setBackgroundResource(R.drawable.bg_settings_card)
                contentView.setTitle("添加 RSS")
                contentView.setSummary("手动输入或粘贴订阅地址")
                contentView.setOnClickListener { onAddRssClick() }
            }

            private fun resetViewState() {
                resetViewState(contentView)
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
        private const val TYPE_PROFILE = 0
        private const val TYPE_DEFAULT = 1
        private const val PRESS_SCALE = 0.97f
        private const val PRESS_DOWN_DURATION_MS = 240L
        private const val PRESS_UP_DURATION_MS = 360L
        private const val PRESS_HOLD_MS = 300L
    }

    private class PressScaleListener(
        private val target: View,
        private val extra: View? = null
    ) : View.OnTouchListener {
        private val slop = ViewConfiguration.get(target.context).scaledTouchSlop
        private val coverView = if (extra != null && extra !== target) extra else null
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
            animateScaleDown(PRESS_DOWN_DURATION_MS)
        }

        private fun scheduleClickHold() {
            clearCallbacks()
            val currentScale = target.scaleX
            val remainingFraction = if (currentScale > PRESS_SCALE) {
                (currentScale - PRESS_SCALE) / (1f - PRESS_SCALE)
            } else {
                0f
            }
            val remainingDuration = (PRESS_DOWN_DURATION_MS * remainingFraction).toLong()
            animateScaleDown(remainingDuration)
            upRunnable = Runnable { animateScaleUp() }
            target.postDelayed(upRunnable, remainingDuration + PRESS_HOLD_MS)
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
            target.animate().cancel()
            target.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(PRESS_UP_DURATION_MS)
                .start()
            coverView?.let { cover ->
                cover.animate().cancel()
                cover.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(PRESS_UP_DURATION_MS)
                    .start()
            }
        }

        private fun animateScaleDown(duration: Long) {
            target.animate().cancel()
            target.animate()
                .scaleX(PRESS_SCALE)
                .scaleY(PRESS_SCALE)
                .setDuration(duration)
                .start()
            coverView?.let { cover ->
                cover.animate().cancel()
                cover.animate()
                    .scaleX(PRESS_SCALE)
                    .scaleY(1f)
                    .setDuration(duration)
                    .start()
            }
        }
    }
}
