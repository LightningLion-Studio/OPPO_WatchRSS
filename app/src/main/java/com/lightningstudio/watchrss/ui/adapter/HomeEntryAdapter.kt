package com.lightningstudio.watchrss.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.heytap.wearable.support.recycler.widget.RecyclerView
import com.heytap.wearable.support.widget.HeyMultipleDefaultItem
import com.lightningstudio.watchrss.R
import com.lightningstudio.watchrss.data.rss.RssChannel
import com.lightningstudio.watchrss.ui.util.formatTime

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
    private val onAddRssClick: () -> Unit
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
                    .inflate(R.layout.item_home_entry, parent, false) as HeyMultipleDefaultItem
                EntryViewHolder.DefaultViewHolder(
                    view,
                    onChannelClick,
                    onChannelLongClick,
                    onAddRssClick
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
            private val itemView: HeyMultipleDefaultItem,
            private val onChannelClick: (RssChannel) -> Unit,
            private val onChannelLongClick: (RssChannel) -> Unit,
            private val onAddRssClick: () -> Unit
        ) : EntryViewHolder(itemView) {
            override fun bind(entry: HomeEntry) {
                resetViewState()
                val indicator = itemView.getMinorImageView()
                indicator?.visibility = View.GONE
                itemView.setOnLongClickListener(null)

                when (entry) {
                    is HomeEntry.Channel -> bindChannel(entry.channel, indicator)
                    HomeEntry.Empty -> bindEmpty()
                    HomeEntry.AddRss -> bindAddRss()
                    HomeEntry.Profile -> Unit
                }
            }

            private fun bindChannel(channel: RssChannel, indicator: android.widget.ImageView?) {
                itemView.setTitle(channel.title)
                val summary = channel.description?.takeIf { it.isNotBlank() } ?: channel.url
                val pinLabel = if (channel.isPinned) "置顶 · " else ""
                val unreadLabel = if (channel.unreadCount > 0) "未读 ${channel.unreadCount} · " else ""
                val timeText = formatTime(channel.lastFetchedAt)
                itemView.setSummary("$pinLabel$summary\n${unreadLabel}更新: $timeText")

                if (channel.unreadCount > 0) {
                    indicator?.visibility = View.VISIBLE
                    indicator?.setImageResource(R.drawable.rss_unread_dot)
                }

                itemView.setOnClickListener { onChannelClick(channel) }
                itemView.setOnLongClickListener {
                    onChannelLongClick(channel)
                    true
                }
            }

            private fun bindEmpty() {
                itemView.setTitle("还没有 RSS 频道")
                itemView.setSummary("点击下方添加你的第一个订阅源")
                itemView.setOnClickListener(null)
            }

            private fun bindAddRss() {
                itemView.setTitle("添加 RSS")
                itemView.setSummary("手动输入或粘贴订阅地址")
                itemView.setOnClickListener { onAddRssClick() }
            }

            private fun resetViewState() {
                itemView.isPressed = false
                itemView.isSelected = false
                itemView.isActivated = false
                itemView.scaleX = 1f
                itemView.scaleY = 1f
                itemView.alpha = 1f
            }
        }
    }

    companion object {
        private const val TYPE_PROFILE = 0
        private const val TYPE_DEFAULT = 1
    }
}
