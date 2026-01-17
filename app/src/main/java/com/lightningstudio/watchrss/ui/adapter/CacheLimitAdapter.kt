package com.lightningstudio.watchrss.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import com.heytap.wearable.support.recycler.widget.RecyclerView
import com.heytap.wearable.support.widget.HeyMultipleItemWithCheckBox
import com.lightningstudio.watchrss.R

class CacheLimitAdapter(
    private val options: List<Long>,
    private val onSelect: (Long) -> Unit
) : RecyclerView.Adapter<CacheLimitAdapter.CacheViewHolder>() {
    private var selected: Long = options.firstOrNull() ?: 0L

    fun updateSelected(value: Long) {
        selected = value
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CacheViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cache_option, parent, false) as HeyMultipleItemWithCheckBox
        return CacheViewHolder(view, onSelect)
    }

    override fun onBindViewHolder(holder: CacheViewHolder, position: Int) {
        holder.bind(options[position], selected)
    }

    override fun getItemCount(): Int = options.size

    class CacheViewHolder(
        private val itemView: HeyMultipleItemWithCheckBox,
        private val onSelect: (Long) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        fun bind(option: Long, selected: Long) {
            itemView.isPressed = false
            itemView.isSelected = false
            itemView.isActivated = false
            itemView.scaleX = 1f
            itemView.scaleY = 1f
            itemView.alpha = 1f
            itemView.setTitle("缓存上限")
            itemView.setSummary("${option}MB")
            itemView.setChecked(option == selected)
            itemView.setOnClickListener { onSelect(option) }
        }
    }
}
