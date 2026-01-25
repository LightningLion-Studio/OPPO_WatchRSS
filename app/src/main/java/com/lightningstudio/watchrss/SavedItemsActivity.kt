package com.lightningstudio.watchrss

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.heytap.wearable.support.widget.HeyToast
import com.lightningstudio.watchrss.data.rss.SaveType
import com.lightningstudio.watchrss.data.rss.SavedItem
import com.lightningstudio.watchrss.ui.screen.rss.SavedItemsScreen
import com.lightningstudio.watchrss.ui.theme.WatchRSSTheme
import com.lightningstudio.watchrss.ui.viewmodel.AppViewModelFactory
import com.lightningstudio.watchrss.ui.viewmodel.SavedItemsViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SavedItemsActivity : BaseHeytapActivity() {
    private val viewModel: SavedItemsViewModel by viewModels {
        AppViewModelFactory((application as WatchRssApplication).container)
    }

    private var lastRemoved by mutableStateOf<SavedItem?>(null)
    private var undoJob: Job? = null
    private val detailLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (viewModel.saveType != SaveType.WATCH_LATER) return@registerForActivityResult
            val itemId = result.data?.getLongExtra(
                DetailActivity.EXTRA_REMOVE_WATCH_LATER_ID,
                0L
            ) ?: 0L
            if (itemId <= 0L) return@registerForActivityResult
            handleAutoRemove(itemId)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        setContent {
            WatchRSSTheme {
                val items by viewModel.items.collectAsState()
                val title = if (viewModel.saveType == SaveType.FAVORITE) "我的收藏" else "稍后再看"
                val hint = "保存在本地，离线可读"
                val emptyMessage = if (viewModel.saveType == SaveType.FAVORITE) "暂无收藏" else "暂无稍后再看"

                SavedItemsScreen(
                    title = title,
                    hint = hint,
                    emptyMessage = emptyMessage,
                    items = items,
                    undoVisible = lastRemoved != null,
                    onUndoClick = { handleUndo() },
                    onItemClick = { savedItem ->
                        if (!allowNavigation()) return@SavedItemsScreen
                        val intent = Intent(this, DetailActivity::class.java)
                        intent.putExtra(DetailActivity.EXTRA_ITEM_ID, savedItem.item.id)
                        intent.putExtra(
                            DetailActivity.EXTRA_FROM_WATCH_LATER,
                            viewModel.saveType == SaveType.WATCH_LATER
                        )
                        detailLauncher.launch(intent)
                    },
                    onItemRemove = { savedItem ->
                        viewModel.toggleSaved(savedItem.item.id)
                        showUndo(savedItem)
                    }
                )
            }
        }
    }

    private fun showUndo(savedItem: SavedItem) {
        lastRemoved = savedItem
        undoJob?.cancel()
        undoJob = lifecycleScope.launch {
            delay(UNDO_TIMEOUT_MS)
            hideUndo()
        }
    }

    private fun handleUndo() {
        val removed = lastRemoved ?: return
        viewModel.toggleSaved(removed.item.id)
        hideUndo()
    }

    private fun hideUndo() {
        undoJob?.cancel()
        undoJob = null
        lastRemoved = null
    }

    private fun handleAutoRemove(itemId: Long) {
        val savedItem = viewModel.items.value.firstOrNull { it.item.id == itemId }
        if (savedItem == null) return
        viewModel.toggleSaved(itemId)
        showUndo(savedItem)
        HeyToast.showToast(this, "已从稍后再看移除", Toast.LENGTH_SHORT)
    }

    companion object {
        const val EXTRA_SAVE_TYPE = "saveType"
        private const val UNDO_TIMEOUT_MS = 3_000L
    }
}
