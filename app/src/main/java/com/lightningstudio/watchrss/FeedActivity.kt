package com.lightningstudio.watchrss

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.heytap.wearable.support.recycler.widget.LinearLayoutManager
import com.heytap.wearable.support.recycler.widget.RecyclerView
import com.heytap.wearable.support.widget.HeyToast
import com.lightningstudio.watchrss.ui.adapter.FeedEntryAdapter
import com.lightningstudio.watchrss.ui.theme.WatchRSSTheme
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

    override fun onSwipeBackAttempt(dx: Float, dy: Float): Boolean {
        return if (::feedAdapter.isInitialized) {
            feedAdapter.closeOpenSwipe()
        } else {
            false
        }
    }

    override fun onResume() {
        super.onResume()
        if (::feedAdapter.isInitialized) {
            feedAdapter.closeOpenSwipe()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        setContent {
            WatchRSSTheme {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { _ ->
                        val root = layoutInflater.inflate(R.layout.activity_feed, null, false)
                        bindViews(root)
                        root
                    }
                )
            }
        }
    }

    private fun bindViews(root: View) {
        val recyclerView = root.findViewById<RecyclerView>(R.id.feed_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        feedAdapter = FeedEntryAdapter(
            scope = lifecycleScope,
            onItemClick = { item ->
                if (feedAdapter.closeOpenSwipe()) return@FeedEntryAdapter
                if (!allowNavigation()) return@FeedEntryAdapter
                val intent = Intent(this, DetailActivity::class.java)
                intent.putExtra(DetailActivity.EXTRA_ITEM_ID, item.id)
                startActivity(intent)
            },
            onItemLongClick = { item ->
                if (!allowNavigation()) return@FeedEntryAdapter
                showItemActions(item)
            },
            onFavoriteClick = { item ->
                feedAdapter.closeOpenSwipe()
                viewModel.toggleFavorite(item.id)
            },
            onWatchLaterClick = { item ->
                feedAdapter.closeOpenSwipe()
                viewModel.toggleWatchLater(item.id)
            },
            onHeaderClick = {
                if (!allowNavigation()) return@FeedEntryAdapter
                openChannelDetail()
            },
            onRefreshClick = { viewModel.refresh() },
            onLoadMoreClick = { viewModel.loadMore() },
            onBackClick = { finish() }
        )
        recyclerView.adapter = feedAdapter
        attachPullToRefresh(recyclerView)

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

    private fun showItemActions(item: com.lightningstudio.watchrss.data.rss.RssItem) {
        val intent = Intent(this, ItemActionsActivity::class.java)
        intent.putExtra(ItemActionsActivity.EXTRA_ITEM_ID, item.id)
        intent.putExtra(ItemActionsActivity.EXTRA_ITEM_TITLE, item.title)
        startActivity(intent)
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
