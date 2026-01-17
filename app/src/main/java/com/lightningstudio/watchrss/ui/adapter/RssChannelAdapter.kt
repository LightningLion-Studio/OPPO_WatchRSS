package com.lightningstudio.watchrss.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import com.heytap.wearable.support.recycler.widget.RecyclerView
import com.heytap.wearable.support.widget.HeyMultipleDefaultItem
import com.lightningstudio.watchrss.R
import com.lightningstudio.watchrss.data.rss.RssChannel
import com.lightningstudio.watchrss.ui.util.formatTime

class RssChannelAdapter(
    private val onClick: (RssChannel) -> Unit
) : RecyclerView.Adapter<RssChannelAdapter.ChannelViewHolder>() {
    private val items = mutableListOf<RssChannel>()

    fun submitList(channels: List<RssChannel>) {
        items.clear()
        items.addAll(channels)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rss_channel, parent, false) as HeyMultipleDefaultItem
        return ChannelViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ChannelViewHolder(
        private val itemView: HeyMultipleDefaultItem,
        private val onClick: (RssChannel) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        fun bind(channel: RssChannel) {
            itemView.isPressed = false
            itemView.isSelected = false
            itemView.isActivated = false
            itemView.scaleX = 1f
            itemView.scaleY = 1f
            itemView.alpha = 1f
            itemView.setTitle(channel.title)
            val summary = channel.description?.takeIf { it.isNotBlank() }
                ?: channel.url
            val timeText = formatTime(channel.lastFetchedAt)
            itemView.setSummary("$summary\n更新: $timeText")
            itemView.setOnClickListener { onClick(channel) }
        }
    }
}
