package com.lightningstudio.watchrss.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.heytap.wearable.support.recycler.widget.RecyclerView
import com.heytap.wearable.support.widget.HeyTextView
import com.lightningstudio.watchrss.R
import com.lightningstudio.watchrss.data.rss.RssRecommendGroup

sealed class RssRecommendEntry {
    data object Header : RssRecommendEntry()
    data class Group(val group: RssRecommendGroup) : RssRecommendEntry()
}

class RssRecommendAdapter(
    private val onGroupClick: (RssRecommendGroup) -> Unit
) : RecyclerView.Adapter<RssRecommendAdapter.EntryViewHolder>() {
    private val entries = mutableListOf<RssRecommendEntry>()

    fun submit(groups: List<RssRecommendGroup>) {
        entries.clear()
        entries.add(RssRecommendEntry.Header)
        groups.forEach { group ->
            entries.add(RssRecommendEntry.Group(group))
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (entries[position]) {
        is RssRecommendEntry.Header -> TYPE_HEADER
        is RssRecommendEntry.Group -> TYPE_GROUP
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> {
                val view = inflater.inflate(R.layout.item_saved_header, parent, false)
                EntryViewHolder.HeaderViewHolder(view)
            }
            TYPE_GROUP -> {
                val view = inflater.inflate(R.layout.item_recommend_group, parent, false)
                EntryViewHolder.GroupViewHolder(view, onGroupClick)
            }
            else -> throw IllegalArgumentException("Unknown viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        holder.bind(entries[position])
    }

    override fun getItemCount(): Int = entries.size

    sealed class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(entry: RssRecommendEntry)

        class HeaderViewHolder(itemView: View) : EntryViewHolder(itemView) {
            private val titleView: HeyTextView = itemView.findViewById(R.id.text_saved_title)
            private val hintView: HeyTextView = itemView.findViewById(R.id.text_saved_hint)

            override fun bind(entry: RssRecommendEntry) {
                if (entry !is RssRecommendEntry.Header) return
                titleView.text = "RSS推荐"
                hintView.text = "点击媒体查看频道"
            }
        }

        class GroupViewHolder(
            itemView: View,
            private val onGroupClick: (RssRecommendGroup) -> Unit
        ) : EntryViewHolder(itemView) {
            private val titleView: HeyTextView = itemView.findViewById(R.id.text_group_title)
            private val descriptionView: HeyTextView = itemView.findViewById(R.id.text_group_description)

            override fun bind(entry: RssRecommendEntry) {
                if (entry !is RssRecommendEntry.Group) return
                val group = entry.group
                titleView.text = group.name
                val description = group.description
                if (description.isNullOrBlank()) {
                    descriptionView.visibility = View.GONE
                } else {
                    descriptionView.visibility = View.VISIBLE
                    descriptionView.text = description
                }
                itemView.setOnClickListener { onGroupClick(group) }
            }
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_GROUP = 1
    }
}
