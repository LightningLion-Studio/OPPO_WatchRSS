package com.lightningstudio.watchrss

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.heytap.wearable.support.recycler.widget.LinearLayoutManager
import com.heytap.wearable.support.recycler.widget.RecyclerView
import com.lightningstudio.watchrss.data.rss.SaveType
import com.lightningstudio.watchrss.ui.adapter.SavedItemAdapter
import com.lightningstudio.watchrss.ui.viewmodel.AppViewModelFactory
import com.lightningstudio.watchrss.ui.viewmodel.SavedItemsViewModel
import kotlinx.coroutines.launch

class SavedItemsActivity : BaseHeytapActivity() {
    private val viewModel: SavedItemsViewModel by viewModels {
        AppViewModelFactory((application as WatchRssApplication).container)
    }

    private lateinit var adapter: SavedItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        setContentView(R.layout.activity_saved_items)

        val recyclerView = findViewById<RecyclerView>(R.id.saved_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = SavedItemAdapter { savedItem ->
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra(DetailActivity.EXTRA_ITEM_ID, savedItem.item.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        val title = if (viewModel.saveType == SaveType.FAVORITE) "我的收藏" else "稍后再看"
        val hint = "保存在本地，离线可读"
        val emptyMessage = if (viewModel.saveType == SaveType.FAVORITE) "暂无收藏" else "暂无稍后再看"

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.items.collect { items ->
                    adapter.submit(title, hint, items, emptyMessage)
                }
            }
        }
    }

    companion object {
        const val EXTRA_SAVE_TYPE = "saveType"
    }
}
