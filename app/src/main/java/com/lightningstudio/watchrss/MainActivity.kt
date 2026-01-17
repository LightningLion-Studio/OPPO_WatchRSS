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
import com.heytap.wearable.support.widget.HeyButton
import com.heytap.wearable.support.widget.HeyTextView
import com.heytap.wearable.support.widget.HeyToast
import com.lightningstudio.watchrss.ui.adapter.RssChannelAdapter
import com.lightningstudio.watchrss.ui.viewmodel.AppViewModelFactory
import com.lightningstudio.watchrss.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

class MainActivity : BaseHeytapActivity() {
    private val viewModel: HomeViewModel by viewModels {
        AppViewModelFactory((application as WatchRssApplication).container)
    }

    private lateinit var channelAdapter: RssChannelAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.channel_list)
        val emptyView = findViewById<HeyTextView>(R.id.empty_view)
        channelAdapter = RssChannelAdapter { channel ->
            val intent = Intent(this, FeedActivity::class.java)
            intent.putExtra(FeedActivity.EXTRA_CHANNEL_ID, channel.id)
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = channelAdapter

        findViewById<HeyButton>(R.id.button_add_rss).setOnClickListener {
            startActivity(Intent(this, AddRssActivity::class.java))
        }
        findViewById<HeyButton>(R.id.button_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.channels.collect { channels ->
                        channelAdapter.submitList(channels)
                        emptyView.visibility = if (channels.isEmpty()) View.VISIBLE else View.GONE
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
}
