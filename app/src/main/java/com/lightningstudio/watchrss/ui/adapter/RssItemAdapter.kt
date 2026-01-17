package com.lightningstudio.watchrss.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.heytap.wearable.support.recycler.widget.RecyclerView
import com.heytap.wearable.support.widget.HeyMultipleDefaultItem
import com.lightningstudio.watchrss.R
import com.lightningstudio.watchrss.data.rss.RssItem

class RssItemAdapter(
    private val onClick: (RssItem) -> Unit
) : RecyclerView.Adapter<RssItemAdapter.ItemViewHolder>() {
    private val items = mutableListOf<RssItem>()

    fun submitList(data: List<RssItem>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rss_item, parent, false) as HeyMultipleDefaultItem
        return ItemViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ItemViewHolder(
        private val itemView: HeyMultipleDefaultItem,
        private val onClick: (RssItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: RssItem) {
            itemView.isPressed = false
            itemView.isSelected = false
            itemView.isActivated = false
            itemView.scaleX = 1f
            itemView.scaleY = 1f
            itemView.alpha = 1f
            itemView.setTitle(item.title)
            val summary = item.description?.takeIf { it.isNotBlank() }
                ?: "暂无摘要"
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

            itemView.setOnClickListener { onClick(item) }
        }
    }
}
