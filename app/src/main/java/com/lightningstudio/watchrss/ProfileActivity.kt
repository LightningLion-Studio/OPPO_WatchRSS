package com.lightningstudio.watchrss

import android.content.Intent
import android.os.Bundle
import com.heytap.wearable.support.widget.HeyButton
import com.heytap.wearable.support.widget.HeyMultipleDefaultItem
import com.heytap.wearable.support.widget.HeyToast

class ProfileActivity : BaseHeytapActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        setContentView(R.layout.activity_profile)

        findViewById<HeyButton>(R.id.button_login).setOnClickListener {
            HeyToast.showToast(this, "暂未接入欢太账号", android.widget.Toast.LENGTH_SHORT)
        }

        val favorites = findViewById<HeyMultipleDefaultItem>(R.id.item_favorites)
        favorites.setTitle("我的收藏")
        favorites.setSummary("RSS / B站 / 抖音")
        favorites.setOnClickListener {
            val intent = Intent(this, SavedItemsActivity::class.java)
            intent.putExtra(SavedItemsActivity.EXTRA_SAVE_TYPE, com.lightningstudio.watchrss.data.rss.SaveType.FAVORITE.name)
            startActivity(intent)
        }

        val watchLater = findViewById<HeyMultipleDefaultItem>(R.id.item_watch_later)
        watchLater.setTitle("稍后再看")
        watchLater.setSummary("跨平台统一列表")
        watchLater.setOnClickListener {
            val intent = Intent(this, SavedItemsActivity::class.java)
            intent.putExtra(SavedItemsActivity.EXTRA_SAVE_TYPE, com.lightningstudio.watchrss.data.rss.SaveType.WATCH_LATER.name)
            startActivity(intent)
        }

        val settings = findViewById<HeyMultipleDefaultItem>(R.id.item_settings)
        settings.setTitle("设置")
        settings.setSummary("阅读与缓存")
        settings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val about = findViewById<HeyMultipleDefaultItem>(R.id.item_about)
        about.setTitle("关于")
        about.setSummary("协议与开源信息")
        about.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }
}
