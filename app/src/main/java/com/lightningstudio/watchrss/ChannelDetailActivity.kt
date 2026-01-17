package com.lightningstudio.watchrss

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.heytap.wearable.support.widget.HeyButton
import com.heytap.wearable.support.widget.HeyDialog
import com.heytap.wearable.support.widget.HeyTextView
import com.lightningstudio.watchrss.ui.util.formatTime
import com.lightningstudio.watchrss.ui.viewmodel.AppViewModelFactory
import com.lightningstudio.watchrss.ui.viewmodel.ChannelDetailViewModel
import kotlinx.coroutines.launch

class ChannelDetailActivity : BaseHeytapActivity() {
    private val viewModel: ChannelDetailViewModel by viewModels {
        AppViewModelFactory((application as WatchRssApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        setContentView(R.layout.activity_channel_detail)

        val titleView = findViewById<HeyTextView>(R.id.text_channel_title)
        val descView = findViewById<HeyTextView>(R.id.text_channel_desc)
        val urlView = findViewById<HeyTextView>(R.id.text_channel_url)
        val timeView = findViewById<HeyTextView>(R.id.text_channel_time)
        val unreadView = findViewById<HeyTextView>(R.id.text_channel_unread)
        val refreshButton = findViewById<HeyButton>(R.id.button_channel_refresh)
        val markReadButton = findViewById<HeyButton>(R.id.button_channel_mark_read)
        val deleteButton = findViewById<HeyButton>(R.id.button_channel_delete)

        refreshButton.setOnClickListener { viewModel.refresh() }
        markReadButton.setOnClickListener { viewModel.markRead() }
        deleteButton.setOnClickListener { confirmDelete() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.channel.collect { channel ->
                    if (channel == null) {
                        titleView.text = "加载中..."
                        descView.visibility = View.GONE
                        urlView.visibility = View.GONE
                        timeView.visibility = View.GONE
                        unreadView.visibility = View.GONE
                        markReadButton.isEnabled = false
                        return@collect
                    }
                    titleView.text = channel.title
                    descView.text = channel.description ?: "暂无简介"
                    descView.visibility = View.VISIBLE
                    urlView.text = channel.url
                    urlView.visibility = View.VISIBLE
                    timeView.text = "更新: ${formatTime(channel.lastFetchedAt)}"
                    timeView.visibility = View.VISIBLE
                    unreadView.text = "未读 ${channel.unreadCount}"
                    unreadView.visibility = View.VISIBLE
                    markReadButton.isEnabled = channel.unreadCount > 0
                    markReadButton.alpha = if (channel.unreadCount > 0) 1f else 0.5f
                }
            }
        }
    }

    private fun confirmDelete() {
        HeyDialog.HeyBuilder(this)
            .setTitle("删除频道")
            .setMessage("删除后将移除本地缓存")
            .setPositiveButton("删除") { _ ->
                viewModel.delete()
                finish()
            }
            .setNegativeButton("取消") { _ -> }
            .create()
            .show()
    }

    companion object {
        const val EXTRA_CHANNEL_ID = "channelId"
    }
}
