package com.lightningstudio.watchrss.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.heytap.wearable.support.recycler.widget.RecyclerView
import com.heytap.wearable.support.widget.HeyMultipleItemWithCheckBox
import com.heytap.wearable.support.widget.HeyTextView
import com.lightningstudio.watchrss.R

class SettingsEntryAdapter(
    private val options: List<Long>,
    private val onSelect: (Long) -> Unit
) : RecyclerView.Adapter<SettingsEntryAdapter.EntryViewHolder>() {
    private var selected: Long = options.firstOrNull() ?: 0L
    private var usageText: String = "当前已用 0MB"

    fun updateSelected(value: Long) {
        selected = value
        notifyDataSetChanged()
    }

    fun updateUsage(usageMb: Long) {
        usageText = "当前已用 ${usageMb}MB"
        notifyItemChanged(0)
    }

    override fun getItemViewType(position: Int): Int = if (position == 0) TYPE_HEADER else TYPE_OPTION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            val view = inflater.inflate(R.layout.item_settings_header, parent, false)
            EntryViewHolder.HeaderViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_cache_option, parent, false) as HeyMultipleItemWithCheckBox
            EntryViewHolder.OptionViewHolder(view, onSelect)
        }
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        if (position == 0) {
            holder.bindHeader(usageText)
        } else {
            holder.bindOption(options[position - 1], selected)
        }
    }

    override fun getItemCount(): Int = options.size + 1

    sealed class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        open fun bindHeader(usageText: String) = Unit
        open fun bindOption(option: Long, selected: Long) = Unit

        class HeaderViewHolder(itemView: View) : EntryViewHolder(itemView) {
            private val usageView: HeyTextView = itemView.findViewById(R.id.text_settings_usage)

            override fun bindHeader(usageText: String) {
                usageView.text = usageText
            }
        }

        class OptionViewHolder(
            private val itemView: HeyMultipleItemWithCheckBox,
            private val onSelect: (Long) -> Unit
        ) : EntryViewHolder(itemView) {
            override fun bindOption(option: Long, selected: Long) {
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

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_OPTION = 1
    }
}
