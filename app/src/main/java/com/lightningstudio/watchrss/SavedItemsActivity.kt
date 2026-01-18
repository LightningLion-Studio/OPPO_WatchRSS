package com.lightningstudio.watchrss

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.heytap.wearable.support.recycler.widget.LinearLayoutManager
import com.heytap.wearable.support.recycler.widget.RecyclerView
import com.heytap.wearable.support.recycler.widget.helper.ItemTouchHelper
import com.heytap.wearable.support.widget.HeyToast
import com.lightningstudio.watchrss.data.rss.SaveType
import com.lightningstudio.watchrss.data.rss.SavedItem
import com.lightningstudio.watchrss.ui.adapter.SavedEntry
import com.lightningstudio.watchrss.ui.adapter.SavedItemAdapter
import com.lightningstudio.watchrss.ui.viewmodel.AppViewModelFactory
import com.lightningstudio.watchrss.ui.viewmodel.SavedItemsViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SavedItemsActivity : BaseHeytapActivity() {
    private val viewModel: SavedItemsViewModel by viewModels {
        AppViewModelFactory((application as WatchRssApplication).container)
    }

    private lateinit var adapter: SavedItemAdapter
    private lateinit var undoButton: ImageButton
    private var lastRemoved: SavedItem? = null
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
        setContentView(R.layout.activity_saved_items)

        val recyclerView = findViewById<RecyclerView>(R.id.saved_list)
        undoButton = findViewById(R.id.button_undo)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = SavedItemAdapter(
            onItemClick = { savedItem ->
                if (!allowNavigation()) return@SavedItemAdapter
                val intent = Intent(this, DetailActivity::class.java)
                intent.putExtra(DetailActivity.EXTRA_ITEM_ID, savedItem.item.id)
                intent.putExtra(
                    DetailActivity.EXTRA_FROM_WATCH_LATER,
                    viewModel.saveType == SaveType.WATCH_LATER
                )
                detailLauncher.launch(intent)
            }
        )
        recyclerView.adapter = adapter
        attachSwipeToDelete(recyclerView)

        val title = if (viewModel.saveType == SaveType.FAVORITE) "我的收藏" else "稍后再看"
        val hint = "保存在本地，离线可读"
        val emptyMessage = if (viewModel.saveType == SaveType.FAVORITE) "暂无收藏" else "暂无稍后再看"

        undoButton.setOnClickListener { handleUndo() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.items.collect { items ->
                    adapter.submit(title, hint, items, emptyMessage)
                }
            }
        }
    }

    private fun showUndo(savedItem: SavedItem) {
        lastRemoved = savedItem
        undoJob?.cancel()
        undoButton.visibility = View.VISIBLE
        undoButton.alpha = 1f
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
        undoButton.visibility = View.GONE
    }

    private fun handleAutoRemove(itemId: Long) {
        val savedItem = adapter.findSavedItem(itemId)
        if (savedItem == null) return
        viewModel.toggleSaved(itemId)
        showUndo(savedItem)
        HeyToast.showToast(this, "已从稍后再看移除", Toast.LENGTH_SHORT)
    }

    private fun attachSwipeToDelete(recyclerView: RecyclerView) {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val entry = adapter.getEntry(viewHolder.adapterPosition)
                return if (entry is SavedEntry.Item) ItemTouchHelper.LEFT else 0
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val entry = adapter.getEntry(viewHolder.adapterPosition) as? SavedEntry.Item ?: return
                val savedItem = entry.item
                viewModel.toggleSaved(savedItem.item.id)
                showUndo(savedItem)
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView)
    }

    companion object {
        const val EXTRA_SAVE_TYPE = "saveType"
        private const val UNDO_TIMEOUT_MS = 3_000L
    }
}
