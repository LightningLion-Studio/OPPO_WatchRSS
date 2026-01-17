package com.lightningstudio.watchrss

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.heytap.wearable.support.recycler.widget.LinearLayoutManager
import com.heytap.wearable.support.recycler.widget.RecyclerView
import com.heytap.wearable.support.widget.HeyToast
import com.lightningstudio.watchrss.data.rss.BuiltinChannelType
import com.lightningstudio.watchrss.data.rss.RssChannel
import com.lightningstudio.watchrss.ui.adapter.HomeEntryAdapter
import com.lightningstudio.watchrss.ui.viewmodel.AppViewModelFactory
import com.lightningstudio.watchrss.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

class MainActivity : BaseHeytapActivity() {
    private val viewModel: HomeViewModel by viewModels {
        AppViewModelFactory((application as WatchRssApplication).container)
    }

    private lateinit var homeAdapter: HomeEntryAdapter

    override fun onSwipeBackAttempt(dx: Float, dy: Float): Boolean {
        return if (::homeAdapter.isInitialized) {
            homeAdapter.closeOpenSwipe()
        } else {
            false
        }
    }

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
                if (homeAdapter.closeOpenSwipe()) return@HomeEntryAdapter
                openChannel(channel)
            },
            onChannelLongClick = { channel ->
                showChannelActions(channel, quick = false)
            },
            onAddRssClick = {
                startActivity(Intent(this, AddRssActivity::class.java))
            },
            onMoveTopClick = { channel ->
                homeAdapter.closeOpenSwipe()
                viewModel.moveToTop(channel)
            },
            onMarkReadClick = { channel ->
                homeAdapter.closeOpenSwipe()
                viewModel.markChannelRead(channel)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = homeAdapter

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

    private fun showChannelActions(channel: RssChannel, quick: Boolean) {
        val intent = Intent(this, ChannelActionsActivity::class.java)
        intent.putExtra(ChannelActionsActivity.EXTRA_CHANNEL_ID, channel.id)
        intent.putExtra(ChannelActionsActivity.EXTRA_QUICK, quick)
        startActivity(intent)
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
