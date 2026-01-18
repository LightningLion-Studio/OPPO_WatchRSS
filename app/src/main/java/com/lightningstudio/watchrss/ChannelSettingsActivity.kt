package com.lightningstudio.watchrss

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.heytap.wearable.support.widget.HeyDialog
import com.heytap.wearable.support.widget.HeySwitch
import com.heytap.wearable.support.widget.HeyTextView
import com.lightningstudio.watchrss.data.rss.BuiltinChannelType
import com.lightningstudio.watchrss.ui.viewmodel.AppViewModelFactory
import com.lightningstudio.watchrss.ui.viewmodel.ChannelDetailViewModel
import kotlinx.coroutines.launch

class ChannelSettingsActivity : BaseHeytapActivity() {
    private val viewModel: ChannelDetailViewModel by viewModels {
        AppViewModelFactory((application as WatchRssApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        setContentView(R.layout.activity_channel_settings)

        val titleView = findViewById<HeyTextView>(R.id.text_settings_title)
        titleView.text = "频道设置"

        val originalContentContainer = findViewById<View>(R.id.layout_channel_original_content)
        val originalContentHint = findViewById<HeyTextView>(R.id.text_channel_original_content_hint)
        val originalContentSwitch = findViewById<HeySwitch>(R.id.switch_channel_original_content)
        val deleteButton = findViewById<View>(R.id.button_channel_delete)
        var currentUseOriginalContent = false

        originalContentSwitch.setOnClickListener {
            viewModel.setOriginalContentEnabled(!currentUseOriginalContent)
        }
        deleteButton.setOnClickListener { confirmDelete() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.channel.collect { channel ->
                    if (channel == null) {
                        originalContentContainer.visibility = View.GONE
                        originalContentHint.visibility = View.GONE
                        deleteButton.isEnabled = false
                        deleteButton.alpha = 0.5f
                        return@collect
                    }
                    deleteButton.isEnabled = true
                    deleteButton.alpha = 1f
                    val isBuiltin = BuiltinChannelType.fromUrl(channel.url) != null
                    if (isBuiltin) {
                        originalContentContainer.visibility = View.GONE
                        originalContentHint.visibility = View.GONE
                    } else {
                        originalContentContainer.visibility = View.VISIBLE
                        originalContentHint.visibility = View.VISIBLE
                        currentUseOriginalContent = channel.useOriginalContent
                        originalContentSwitch.isChecked = channel.useOriginalContent
                        originalContentSwitch.isEnabled = true
                    }
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
        const val EXTRA_CHANNEL_ID = ChannelDetailActivity.EXTRA_CHANNEL_ID
    }
}
