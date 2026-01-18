package com.lightningstudio.watchrss.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.heytap.wearable.support.recycler.widget.RecyclerView
import com.heytap.wearable.support.widget.HeyTextView
import com.lightningstudio.watchrss.R
import com.lightningstudio.watchrss.data.rss.RssRecommendChannel
import com.lightningstudio.watchrss.data.rss.RssRecommendGroup

sealed class RssRecommendChannelEntry {
    data class Header(val title: String, val hint: String) : RssRecommendChannelEntry()
    data class Channel(val channel: RssRecommendChannel) : RssRecommendChannelEntry()
}

class RssRecommendChannelAdapter(
    private val onAddChannel: (String) -> Unit
) : RecyclerView.Adapter<RssRecommendChannelAdapter.EntryViewHolder>() {
    private val entries = mutableListOf<RssRecommendChannelEntry>()

    fun submit(group: RssRecommendGroup) {
        entries.clear()
        entries.add(RssRecommendChannelEntry.Header("RSS推荐", group.name))
        entries.addAll(group.channels.map { channel ->
            RssRecommendChannelEntry.Channel(channel)
        })
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (entries[position]) {
        is RssRecommendChannelEntry.Header -> TYPE_HEADER
        is RssRecommendChannelEntry.Channel -> TYPE_CHANNEL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> {
                val view = inflater.inflate(R.layout.item_saved_header, parent, false)
                EntryViewHolder.HeaderViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_recommend_channel, parent, false)
                EntryViewHolder.ChannelViewHolder(view, onAddChannel)
            }
        }
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        holder.bind(entries[position])
    }

    override fun getItemCount(): Int = entries.size

    sealed class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(entry: RssRecommendChannelEntry)

        class HeaderViewHolder(itemView: View) : EntryViewHolder(itemView) {
            private val titleView: HeyTextView = itemView.findViewById(R.id.text_saved_title)
            private val hintView: HeyTextView = itemView.findViewById(R.id.text_saved_hint)

            override fun bind(entry: RssRecommendChannelEntry) {
                if (entry !is RssRecommendChannelEntry.Header) return
                titleView.text = entry.title
                hintView.text = entry.hint
            }
        }

        class ChannelViewHolder(
            itemView: View,
            private val onAddChannel: (String) -> Unit
        ) : EntryViewHolder(itemView) {
            private val titleView: HeyTextView = itemView.findViewById(R.id.text_channel_title)
            private val urlView: HeyTextView = itemView.findViewById(R.id.text_channel_url)
            private val addButton: View = itemView.findViewById(R.id.button_add_channel)

            override fun bind(entry: RssRecommendChannelEntry) {
                if (entry !is RssRecommendChannelEntry.Channel) return
                val channel = entry.channel
                titleView.text = channel.title
                urlView.text = channel.url
                addButton.setOnClickListener { onAddChannel(channel.url) }
            }
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_CHANNEL = 1
    }
}
