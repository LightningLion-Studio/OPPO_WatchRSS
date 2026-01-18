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
import com.lightningstudio.watchrss.data.rss.BuiltinChannelType
import com.lightningstudio.watchrss.ui.viewmodel.AppViewModelFactory
import com.lightningstudio.watchrss.ui.viewmodel.ChannelActionsViewModel
import kotlinx.coroutines.launch

class ChannelActionsActivity : BaseHeytapActivity() {
    private val viewModel: ChannelActionsViewModel by viewModels {
        AppViewModelFactory((application as WatchRssApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        setContentView(R.layout.dialog_home_actions)

        if (!viewModel.isValid()) {
            finish()
            return
        }

        val moveTopButton = findViewById<HeyButton>(R.id.button_move_top)
        val pinButton = findViewById<HeyButton>(R.id.button_pin)
        val markReadButton = findViewById<HeyButton>(R.id.button_mark_read)
        val deleteButton = findViewById<HeyButton>(R.id.button_delete)
        val cancelButton = findViewById<HeyButton>(R.id.button_cancel)

        val quick = intent.getBooleanExtra(EXTRA_QUICK, false)
        deleteButton.visibility = if (quick) View.GONE else View.VISIBLE

        moveTopButton.setOnClickListener {
            viewModel.moveToTop()
            finish()
        }
        pinButton.setOnClickListener {
            viewModel.togglePinned()
            finish()
        }
        markReadButton.setOnClickListener {
            viewModel.markRead()
            finish()
        }
        deleteButton.setOnClickListener { confirmDelete() }
        cancelButton.setOnClickListener { finish() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.channel.collect { channel ->
                    if (channel == null) {
                        moveTopButton.isEnabled = false
                        pinButton.isEnabled = false
                        markReadButton.isEnabled = false
                        markReadButton.alpha = 0.5f
                        deleteButton.isEnabled = false
                        deleteButton.alpha = 0.5f
                        return@collect
                    }
                    moveTopButton.isEnabled = true
                    pinButton.isEnabled = true
                    deleteButton.isEnabled = true
                    deleteButton.alpha = 1f
                    pinButton.text = if (channel.isPinned) "取消置顶" else "置顶"
                    val isBuiltin = BuiltinChannelType.fromUrl(channel.url) != null
                    val canMarkRead = channel.unreadCount > 0 && !isBuiltin
                    markReadButton.isEnabled = canMarkRead
                    markReadButton.alpha = if (canMarkRead) 1f else 0.5f
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
        const val EXTRA_QUICK = "quick"
    }
}
