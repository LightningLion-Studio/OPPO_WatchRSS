package com.lightningstudio.watchrss

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import com.heytap.wearable.support.widget.HeyToast
import com.lightningstudio.watchrss.data.rss.RssRecommendations
import com.lightningstudio.watchrss.ui.screen.rss.RssRecommendGroupScreen
import com.lightningstudio.watchrss.ui.theme.WatchRSSTheme

class RssRecommendGroupActivity : BaseHeytapActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()

        val groupId = intent.getStringExtra(EXTRA_GROUP_ID).orEmpty()
        val group = RssRecommendations.findGroup(groupId)
        if (group == null) {
            HeyToast.showToast(this, "未找到推荐频道", android.widget.Toast.LENGTH_SHORT)
            finish()
            return
        }

        setContent {
            WatchRSSTheme {
                RssRecommendGroupScreen(
                    group = group,
                    onAddChannel = { channel ->
                        if (!allowNavigation()) return@RssRecommendGroupScreen
                        val intent = Intent(this, AddRssActivity::class.java)
                        intent.putExtra(AddRssActivity.EXTRA_URL, channel.url)
                        startActivity(intent)
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_GROUP_ID = "extra_group_id"
    }
}
