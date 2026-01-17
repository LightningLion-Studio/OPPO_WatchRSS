package com.lightningstudio.watchrss

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.heytap.wearable.support.recycler.widget.LinearLayoutManager
import com.heytap.wearable.support.recycler.widget.RecyclerView
import com.heytap.wearable.support.recycler.widget.helper.ItemTouchHelper
import com.heytap.wearable.support.widget.HeyButton
import com.heytap.wearable.support.widget.HeyDialog
import com.heytap.wearable.support.widget.HeyToast
import com.lightningstudio.watchrss.data.rss.RssChannel
import com.lightningstudio.watchrss.ui.adapter.HomeEntry
import com.lightningstudio.watchrss.ui.adapter.HomeEntryAdapter
import com.lightningstudio.watchrss.ui.adapter.PlatformType
import com.lightningstudio.watchrss.ui.viewmodel.AppViewModelFactory
import com.lightningstudio.watchrss.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

class MainActivity : BaseHeytapActivity() {
    private val viewModel: HomeViewModel by viewModels {
        AppViewModelFactory((application as WatchRssApplication).container)
    }

    private lateinit var homeAdapter: HomeEntryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.channel_list)
        homeAdapter = HomeEntryAdapter(
            onProfileClick = {
                startActivity(Intent(this, ProfileActivity::class.java))
            },
            onChannelClick = { channel ->
                val intent = Intent(this, FeedActivity::class.java)
                intent.putExtra(FeedActivity.EXTRA_CHANNEL_ID, channel.id)
                startActivity(intent)
            },
            onChannelLongClick = { channel ->
                showChannelActions(channel, quick = false)
            },
            onPlatformClick = { platform ->
                when (platform) {
                    PlatformType.BILI -> startActivity(Intent(this, BiliEntryActivity::class.java))
                    PlatformType.DOUYIN -> startActivity(Intent(this, DouyinEntryActivity::class.java))
                }
            },
            onAddRssClick = {
                startActivity(Intent(this, AddRssActivity::class.java))
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = homeAdapter
        attachQuickSwipe(recyclerView)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.channels.collect { channels ->
                        homeAdapter.submitList(channels)
                    }
                }
                launch {
                    viewModel.message.collect { message ->
                        if (message != null) {
                            HeyToast.showToast(this@MainActivity, message, android.widget.Toast.LENGTH_SHORT)
                            viewModel.clearMessage()
                        }
                    }
                }
            }
        }
    }

    private fun attachQuickSwipe(recyclerView: RecyclerView) {
        val helper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val entry = homeAdapter.getEntry(viewHolder.adapterPosition)
                return if (entry is HomeEntry.Channel) ItemTouchHelper.LEFT else 0
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position == RecyclerView.NO_POSITION) return
                val entry = homeAdapter.getEntry(position)
                if (entry is HomeEntry.Channel) {
                    showChannelActions(entry.channel, quick = true)
                }
                homeAdapter.notifyItemChanged(position)
            }
        })
        helper.attachToRecyclerView(recyclerView)
    }

    private fun showChannelActions(channel: RssChannel, quick: Boolean) {
        val dialog = HeyDialog.HeyBuilder(this)
            .setTitle(channel.title)
            .setContentView(R.layout.dialog_home_actions)
            .create()
        dialog.show()

        val moveTopButton = dialog.findViewById<HeyButton>(R.id.button_move_top)
        val pinButton = dialog.findViewById<HeyButton>(R.id.button_pin)
        val markReadButton = dialog.findViewById<HeyButton>(R.id.button_mark_read)
        val deleteButton = dialog.findViewById<HeyButton>(R.id.button_delete)

        moveTopButton?.setOnClickListener {
            viewModel.moveToTop(channel)
            dialog.dismiss()
        }

        pinButton?.text = if (channel.isPinned) "取消置顶" else "置顶"
        pinButton?.setOnClickListener {
            viewModel.togglePinned(channel)
            dialog.dismiss()
        }

        markReadButton?.isEnabled = channel.unreadCount > 0
        markReadButton?.alpha = if (channel.unreadCount > 0) 1f else 0.5f
        markReadButton?.setOnClickListener {
            viewModel.markChannelRead(channel)
            dialog.dismiss()
        }

        deleteButton?.visibility = if (quick) View.GONE else View.VISIBLE
        deleteButton?.setOnClickListener {
            dialog.dismiss()
            confirmDelete(channel)
        }
    }

    private fun confirmDelete(channel: RssChannel) {
        HeyDialog.HeyBuilder(this)
            .setTitle("删除频道")
            .setMessage("删除后将移除本地缓存")
            .setPositiveButton("删除") { _ ->
                viewModel.deleteChannel(channel)
            }
            .setNegativeButton("取消") { _ -> }
            .create()
            .show()
    }
}
