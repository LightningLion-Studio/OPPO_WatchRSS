package com.lightningstudio.watchrss

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.heytap.wearable.support.widget.HeyButton
import com.heytap.wearable.support.widget.HeyTextView
import com.lightningstudio.watchrss.ui.viewmodel.AppViewModelFactory
import com.lightningstudio.watchrss.ui.viewmodel.ItemActionsViewModel
import kotlinx.coroutines.launch

class ItemActionsActivity : BaseHeytapActivity() {
    private val viewModel: ItemActionsViewModel by viewModels {
        AppViewModelFactory((application as WatchRssApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        setContentView(R.layout.dialog_feed_actions)

        if (!viewModel.isValid()) {
            finish()
            return
        }

        val titleView = findViewById<HeyTextView>(R.id.dialog_title)
        val favoriteButton = findViewById<HeyButton>(R.id.button_favorite)
        val laterButton = findViewById<HeyButton>(R.id.button_watch_later)
        val cancelButton = findViewById<HeyButton>(R.id.button_cancel)

        val initialTitle = intent.getStringExtra(EXTRA_ITEM_TITLE)
        if (!initialTitle.isNullOrBlank()) {
            titleView.text = initialTitle
        } else {
            titleView.text = "加载中..."
        }

        favoriteButton.setOnClickListener {
            viewModel.toggleFavorite()
            finish()
        }
        laterButton.setOnClickListener {
            viewModel.toggleWatchLater()
            finish()
        }
        cancelButton.setOnClickListener { finish() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.item.collect { item ->
                        if (item != null) {
                            titleView.text = item.title
                        }
                    }
                }
                launch {
                    viewModel.savedState.collect { state ->
                        favoriteButton.text = if (state.isFavorite) "取消收藏" else "收藏"
                        laterButton.text = if (state.isWatchLater) "取消稍后再看" else "稍后再看"
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_ITEM_ID = "itemId"
        const val EXTRA_ITEM_TITLE = "itemTitle"
    }
}
