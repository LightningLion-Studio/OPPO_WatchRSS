package com.lightningstudio.watchrss

import android.content.Intent
import android.os.Bundle
import com.heytap.wearable.support.widget.HeyToast

class ProfileActivity : BaseHeytapActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        setContentView(R.layout.activity_profile)

        findViewById<android.view.View>(R.id.account_entry).setOnClickListener {
            HeyToast.showToast(this, "暂未接入欢太账号", android.widget.Toast.LENGTH_SHORT)
        }

        val favorites = findViewById<android.view.View>(R.id.item_favorites)
        favorites.setOnClickListener {
            val intent = Intent(this, SavedItemsActivity::class.java)
            intent.putExtra(SavedItemsActivity.EXTRA_SAVE_TYPE, com.lightningstudio.watchrss.data.rss.SaveType.FAVORITE.name)
            startActivity(intent)
        }

        val watchLater = findViewById<android.view.View>(R.id.item_watch_later)
        watchLater.setOnClickListener {
            val intent = Intent(this, SavedItemsActivity::class.java)
            intent.putExtra(SavedItemsActivity.EXTRA_SAVE_TYPE, com.lightningstudio.watchrss.data.rss.SaveType.WATCH_LATER.name)
            startActivity(intent)
        }

        val settings = findViewById<android.view.View>(R.id.item_settings)
        settings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val about = findViewById<android.view.View>(R.id.item_about)
        about.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }
}
