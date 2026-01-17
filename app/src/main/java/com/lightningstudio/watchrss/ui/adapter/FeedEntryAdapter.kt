package com.lightningstudio.watchrss.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.heytap.wearable.support.recycler.widget.RecyclerView
import com.heytap.wearable.support.widget.HeyMultipleDefaultItem
import com.heytap.wearable.support.widget.HeyTextView
import com.lightningstudio.watchrss.R
import com.lightningstudio.watchrss.data.rss.RssItem

sealed class FeedEntry {
    data class Header(val title: String, val isRefreshing: Boolean) : FeedEntry()
    data class Item(val item: RssItem) : FeedEntry()
    data object Empty : FeedEntry()
}

class FeedEntryAdapter(
    private val onItemClick: (RssItem) -> Unit
) : RecyclerView.Adapter<FeedEntryAdapter.EntryViewHolder>() {
    private val entries = mutableListOf<FeedEntry>()

    fun submit(title: String, items: List<RssItem>, isRefreshing: Boolean) {
        entries.clear()
        entries.add(FeedEntry.Header(title, isRefreshing))
        if (items.isEmpty()) {
            entries.add(FeedEntry.Empty)
        } else {
            entries.addAll(items.map { FeedEntry.Item(it) })
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
    }

    override fun getItemViewType(position: Int): Int = when (entries[position]) {
        is FeedEntry.Header -> TYPE_HEADER
        is FeedEntry.Empty -> TYPE_EMPTY
        is FeedEntry.Item -> TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> {
                val view = inflater.inflate(R.layout.item_feed_header, parent, false)
                EntryViewHolder.HeaderViewHolder(view)
            }
            TYPE_EMPTY -> {
                val view = inflater.inflate(R.layout.item_feed_empty, parent, false)
                EntryViewHolder.EmptyViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_rss_item, parent, false) as HeyMultipleDefaultItem
                EntryViewHolder.ItemViewHolder(view, onItemClick)
            }
        }
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        holder.bind(entries[position])
    }

    override fun getItemCount(): Int = entries.size

    sealed class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(entry: FeedEntry)

        class HeaderViewHolder(itemView: View) : EntryViewHolder(itemView) {
            private val titleView: HeyTextView = itemView.findViewById(R.id.text_feed_title)
            private val hintView: HeyTextView = itemView.findViewById(R.id.text_feed_hint)

            override fun bind(entry: FeedEntry) {
                if (entry is FeedEntry.Header) {
                    titleView.text = entry.title
                    hintView.text = if (entry.isRefreshing) "正在刷新中..." else "下拉刷新"
                }
            }
        }

        class EmptyViewHolder(itemView: View) : EntryViewHolder(itemView) {
            override fun bind(entry: FeedEntry) = Unit
        }

        class ItemViewHolder(
            private val itemView: HeyMultipleDefaultItem,
            private val onItemClick: (RssItem) -> Unit
        ) : EntryViewHolder(itemView) {
            override fun bind(entry: FeedEntry) {
                if (entry !is FeedEntry.Item) return
                val item = entry.item
                resetViewState()
                itemView.setTitle(item.title)
                val summary = item.description?.takeIf { it.isNotBlank() } ?: "暂无摘要"
                itemView.setSummary(summary)

                val indicator = itemView.getMinorImageView()
                if (indicator != null) {
                    if (item.isRead) {
                        indicator.visibility = View.GONE
                    } else {
                        indicator.visibility = View.VISIBLE
                        indicator.setImageResource(R.drawable.rss_unread_dot)
                    }
                }

                itemView.setOnClickListener { onItemClick(item) }
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
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
        private const val TYPE_EMPTY = 2
    }
}
