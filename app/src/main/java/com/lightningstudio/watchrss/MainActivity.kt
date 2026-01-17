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
import com.lightningstudio.watchrss.data.rss.BuiltinChannelType
import com.lightningstudio.watchrss.ui.adapter.HomeEntry
import com.lightningstudio.watchrss.ui.adapter.HomeEntryAdapter
import com.lightningstudio.watchrss.ui.util.SwipeRevealCallback
import com.lightningstudio.watchrss.ui.viewmodel.AppViewModelFactory
import com.lightningstudio.watchrss.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

class MainActivity : BaseHeytapActivity() {
    private val viewModel: HomeViewModel by viewModels {
        AppViewModelFactory((application as WatchRssApplication).container)
    }

    private lateinit var homeAdapter: HomeEntryAdapter
    private lateinit var swipeHelper: SwipeRevealCallback

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
                if (::swipeHelper.isInitialized && swipeHelper.closeOpenItem()) return@HomeEntryAdapter
                openChannel(channel)
            },
            onChannelLongClick = { channel ->
                showChannelActions(channel, quick = false)
            },
            onAddRssClick = {
                startActivity(Intent(this, AddRssActivity::class.java))
            },
            onMoveTopClick = { channel ->
                if (::swipeHelper.isInitialized) {
                    swipeHelper.closeOpenItem()
                }
                viewModel.moveToTop(channel)
            },
            onMarkReadClick = { channel ->
                if (::swipeHelper.isInitialized) {
                    swipeHelper.closeOpenItem()
                }
                viewModel.markChannelRead(channel)
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
        swipeHelper = SwipeRevealCallback(recyclerView) { viewHolder ->
            val entry = homeAdapter.getEntry(viewHolder.adapterPosition)
            entry is HomeEntry.Channel
        }
        ItemTouchHelper(swipeHelper).attachToRecyclerView(recyclerView)
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
        val isBuiltin = BuiltinChannelType.fromUrl(channel.url) != null
        if (isBuiltin) {
            markReadButton?.isEnabled = false
            markReadButton?.alpha = 0.5f
            markReadButton?.setOnClickListener(null)
        } else {
            markReadButton?.alpha = if (channel.unreadCount > 0) 1f else 0.5f
            markReadButton?.setOnClickListener {
                viewModel.markChannelRead(channel)
                dialog.dismiss()
            }
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

    private fun openChannel(channel: RssChannel) {
        when (BuiltinChannelType.fromUrl(channel.url)) {
            BuiltinChannelType.BILI -> startActivity(Intent(this, BiliEntryActivity::class.java))
            BuiltinChannelType.DOUYIN -> startActivity(Intent(this, DouyinEntryActivity::class.java))
            null -> {
                val intent = Intent(this, FeedActivity::class.java)
                intent.putExtra(FeedActivity.EXTRA_CHANNEL_ID, channel.id)
                startActivity(intent)
            }
        }
    }
}
