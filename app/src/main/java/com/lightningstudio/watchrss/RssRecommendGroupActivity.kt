package com.lightningstudio.watchrss

import android.content.Intent
import android.os.Bundle
import com.heytap.wearable.support.recycler.widget.LinearLayoutManager
import com.heytap.wearable.support.recycler.widget.RecyclerView
import com.heytap.wearable.support.widget.HeyToast
import com.lightningstudio.watchrss.data.rss.RssRecommendations
import com.lightningstudio.watchrss.ui.adapter.RssRecommendChannelAdapter

class RssRecommendGroupActivity : BaseHeytapActivity() {
    private lateinit var adapter: RssRecommendChannelAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        setContentView(R.layout.activity_rss_recommend_group)

        val groupId = intent.getStringExtra(EXTRA_GROUP_ID).orEmpty()
        val group = RssRecommendations.findGroup(groupId)
        if (group == null) {
            HeyToast.showToast(this, "未找到推荐频道", android.widget.Toast.LENGTH_SHORT)
            finish()
            return
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recommend_group_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RssRecommendChannelAdapter { url ->
            if (!allowNavigation()) return@RssRecommendChannelAdapter
            val intent = Intent(this, AddRssActivity::class.java)
            intent.putExtra(AddRssActivity.EXTRA_URL, url)
            startActivity(intent)
        }
        recyclerView.adapter = adapter
        adapter.submit(group)
    }

    companion object {
        const val EXTRA_GROUP_ID = "extra_group_id"
    }
}
