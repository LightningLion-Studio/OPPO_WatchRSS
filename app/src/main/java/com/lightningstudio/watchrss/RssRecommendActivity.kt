package com.lightningstudio.watchrss

import android.content.Intent
import android.os.Bundle
import com.heytap.wearable.support.recycler.widget.LinearLayoutManager
import com.heytap.wearable.support.recycler.widget.RecyclerView
import com.lightningstudio.watchrss.data.rss.RssRecommendations
import com.lightningstudio.watchrss.ui.adapter.RssRecommendAdapter

class RssRecommendActivity : BaseHeytapActivity() {
    private lateinit var adapter: RssRecommendAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        setContentView(R.layout.activity_rss_recommend)

        val recyclerView = findViewById<RecyclerView>(R.id.recommend_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RssRecommendAdapter { group ->
            if (!allowNavigation()) return@RssRecommendAdapter
            val intent = Intent(this, RssRecommendGroupActivity::class.java)
            intent.putExtra(RssRecommendGroupActivity.EXTRA_GROUP_ID, group.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter
        adapter.submit(RssRecommendations.groups)
    }
}
