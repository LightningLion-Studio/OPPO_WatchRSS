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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        setContentView(R.layout.activity_feed)

        val recyclerView = findViewById<RecyclerView>(R.id.feed_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        feedAdapter = FeedEntryAdapter { item ->
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra(DetailActivity.EXTRA_ITEM_ID, item.id)
            startActivity(intent)
        }
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
                        feedAdapter.submit(currentTitle, items, isRefreshing)
                    }
                }
                launch {
                    viewModel.isRefreshing.collect { refreshing ->
                        isRefreshing = refreshing
                        feedAdapter.updateRefreshing(refreshing)
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

    companion object {
        const val EXTRA_CHANNEL_ID = "channelId"
    }
}
