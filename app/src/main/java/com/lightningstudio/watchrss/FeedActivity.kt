package com.lightningstudio.watchrss

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.heytap.wearable.support.recycler.widget.LinearLayoutManager
import com.heytap.wearable.support.recycler.widget.RecyclerView
import com.heytap.wearable.support.recycler.widget.helper.ItemTouchHelper
import com.heytap.wearable.support.widget.HeyToast
import com.lightningstudio.watchrss.ui.adapter.FeedEntryAdapter
import com.lightningstudio.watchrss.ui.viewmodel.AppViewModelFactory
import com.lightningstudio.watchrss.ui.viewmodel.FeedViewModel
import kotlinx.coroutines.launch

class FeedActivity : BaseHeytapActivity() {
    private val viewModel: FeedViewModel by viewModels {
        AppViewModelFactory((application as WatchRssApplication).container)
    }

    private lateinit var feedAdapter: FeedEntryAdapter
    private var currentTitle: String = "RSS"
    private var isRefreshing: Boolean = false
    private var canLoadMore: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        setContentView(R.layout.activity_feed)

        val recyclerView = findViewById<RecyclerView>(R.id.feed_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        feedAdapter = FeedEntryAdapter(
            onItemClick = { item ->
                val intent = Intent(this, DetailActivity::class.java)
                intent.putExtra(DetailActivity.EXTRA_ITEM_ID, item.id)
                startActivity(intent)
            },
            onItemLongClick = { item ->
                showItemActions(item)
            },
            onHeaderClick = { openChannelDetail() },
            onRefreshClick = { viewModel.refresh() },
            onLoadMoreClick = { viewModel.loadMore() },
            onBackClick = { finish() }
        )
        recyclerView.adapter = feedAdapter
        attachPullToRefresh(recyclerView)
        attachQuickSwipe(recyclerView)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.channel.collect { channel ->
                        currentTitle = channel?.title ?: "RSS"
                        feedAdapter.updateTitle(currentTitle)
                    }
                }
                launch {
                    viewModel.items.collect { items ->
                        feedAdapter.submit(currentTitle, items, isRefreshing, canLoadMore)
                    }
                }
                launch {
                    viewModel.isRefreshing.collect { refreshing ->
                        isRefreshing = refreshing
                        feedAdapter.updateRefreshing(refreshing)
                    }
                }
                launch {
                    viewModel.hasMore.collect { hasMore ->
                        canLoadMore = hasMore
                        feedAdapter.updateHasMore(hasMore)
                    }
                }
                launch {
                    viewModel.message.collect { message ->
                        if (message != null) {
                            HeyToast.showToast(this@FeedActivity, message, android.widget.Toast.LENGTH_SHORT)
                            viewModel.clearMessage()
                        }
                    }
                }
            }
        }
    }

    private fun attachPullToRefresh(recyclerView: RecyclerView) {
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        val thresholdPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            48f,
            resources.displayMetrics
        )
        var startY = 0f
        var triggered = false

        recyclerView.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.y
                    triggered = false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (triggered || isRefreshing) return@setOnTouchListener false
                    val dy = event.y - startY
                    if (dy > touchSlop && dy > thresholdPx && !recyclerView.canScrollVertically(-1)) {
                        triggered = true
                        viewModel.refresh()
                    }
                }
                MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_UP -> {
                    startY = 0f
                    triggered = false
                }
            }
            false
        }
    }

    private fun attachQuickSwipe(recyclerView: RecyclerView) {
        val helper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val entry = feedAdapter.getEntry(viewHolder.adapterPosition)
                return if (entry is com.lightningstudio.watchrss.ui.adapter.FeedEntry.Item) {
                    ItemTouchHelper.LEFT
                } else {
                    0
                }
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position == RecyclerView.NO_POSITION) return
                val entry = feedAdapter.getEntry(position)
                if (entry is com.lightningstudio.watchrss.ui.adapter.FeedEntry.Item) {
                    showItemActions(entry.item)
                }
                feedAdapter.notifyItemChanged(position)
            }
        })
        helper.attachToRecyclerView(recyclerView)
    }

    private fun showItemActions(item: com.lightningstudio.watchrss.data.rss.RssItem) {
        lifecycleScope.launch {
            val state = viewModel.getSavedState(item.id)
            val dialog = com.heytap.wearable.support.widget.HeyDialog.HeyBuilder(this@FeedActivity)
                .setTitle(item.title)
                .setContentView(R.layout.dialog_feed_actions)
                .create()
            dialog.show()

            val favoriteButton = dialog.findViewById<com.heytap.wearable.support.widget.HeyButton>(R.id.button_favorite)
            val laterButton = dialog.findViewById<com.heytap.wearable.support.widget.HeyButton>(R.id.button_watch_later)
            favoriteButton?.text = if (state.isFavorite) "取消收藏" else "收藏"
            laterButton?.text = if (state.isWatchLater) "取消稍后再看" else "稍后再看"

            favoriteButton?.setOnClickListener {
                viewModel.toggleFavorite(item.id)
                dialog.dismiss()
            }
            laterButton?.setOnClickListener {
                viewModel.toggleWatchLater(item.id)
                dialog.dismiss()
            }
        }
    }

    private fun openChannelDetail() {
        val channelId = intent.getLongExtra(EXTRA_CHANNEL_ID, 0L)
        if (channelId <= 0L) return
        val intent = Intent(this, ChannelDetailActivity::class.java)
        intent.putExtra(ChannelDetailActivity.EXTRA_CHANNEL_ID, channelId)
        startActivity(intent)
    }

    companion object {
        const val EXTRA_CHANNEL_ID = "channelId"
    }
}
