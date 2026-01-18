package com.lightningstudio.watchrss.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.heytap.wearable.support.recycler.widget.RecyclerView
import com.heytap.wearable.support.widget.HeyMultipleDefaultItem
import com.heytap.wearable.support.widget.HeyTextView
import com.lightningstudio.watchrss.R
import com.lightningstudio.watchrss.data.rss.SavedItem
import com.lightningstudio.watchrss.ui.util.formatRssSummary
import com.lightningstudio.watchrss.ui.util.formatTime

sealed class SavedEntry {
    data class Header(val title: String, val hint: String) : SavedEntry()
    data class Item(val item: SavedItem) : SavedEntry()
    data class Empty(val message: String) : SavedEntry()
}

class SavedItemAdapter(
    private val onItemClick: (SavedItem) -> Unit
) : RecyclerView.Adapter<SavedItemAdapter.EntryViewHolder>() {
    private val entries = mutableListOf<SavedEntry>()

    fun submit(title: String, hint: String, items: List<SavedItem>, emptyMessage: String) {
        entries.clear()
        entries.add(SavedEntry.Header(title, hint))
        if (items.isEmpty()) {
            entries.add(SavedEntry.Empty(emptyMessage))
        } else {
            entries.addAll(items.map { SavedEntry.Item(it) })
        }
        notifyDataSetChanged()
    }

    fun getEntry(position: Int): SavedEntry? = entries.getOrNull(position)

    fun findSavedItem(itemId: Long): SavedItem? {
        return entries.asSequence()
            .filterIsInstance<SavedEntry.Item>()
            .firstOrNull { it.item.item.id == itemId }
            ?.item
    }

    override fun getItemViewType(position: Int): Int = when (entries[position]) {
        is SavedEntry.Header -> TYPE_HEADER
        is SavedEntry.Empty -> TYPE_EMPTY
        is SavedEntry.Item -> TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> {
                val view = inflater.inflate(R.layout.item_saved_header, parent, false)
                EntryViewHolder.HeaderViewHolder(view)
            }
            TYPE_EMPTY -> {
                val view = inflater.inflate(R.layout.item_saved_empty, parent, false)
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
        abstract fun bind(entry: SavedEntry)

        class HeaderViewHolder(itemView: View) : EntryViewHolder(itemView) {
            private val titleView: HeyTextView = itemView.findViewById(R.id.text_saved_title)
            private val hintView: HeyTextView = itemView.findViewById(R.id.text_saved_hint)

            override fun bind(entry: SavedEntry) {
                if (entry is SavedEntry.Header) {
                    titleView.text = entry.title
                    hintView.text = entry.hint
                }
            }
        }

        class EmptyViewHolder(itemView: View) : EntryViewHolder(itemView) {
            private val emptyView: HeyTextView = itemView.findViewById(R.id.text_saved_empty)

            override fun bind(entry: SavedEntry) {
                if (entry is SavedEntry.Empty) {
                    emptyView.text = entry.message
                }
            }
        }

        class ItemViewHolder(
            private val itemView: HeyMultipleDefaultItem,
            private val onItemClick: (SavedItem) -> Unit
        ) : EntryViewHolder(itemView) {
            override fun bind(entry: SavedEntry) {
                if (entry !is SavedEntry.Item) return
                val saved = entry.item
                itemView.isPressed = false
                itemView.isSelected = false
                itemView.isActivated = false
                itemView.scaleX = 1f
                itemView.scaleY = 1f
                itemView.alpha = 1f
                itemView.setTitle(saved.item.title)
                val summary = formatRssSummary(saved.item.description) ?: "暂无摘要"
                val meta = "${saved.channelTitle} · ${formatTime(saved.savedAt)}"
                itemView.setSummary("$meta\n$summary")
                itemView.setOnClickListener { onItemClick(saved) }
            }
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
        private const val TYPE_EMPTY = 2
    }
}
