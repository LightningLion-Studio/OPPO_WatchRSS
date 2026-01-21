package com.lightningstudio.watchrss

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import com.heytap.wearable.support.widget.HeyToast
import com.lightningstudio.watchrss.ui.screen.ProfileScreen
import com.lightningstudio.watchrss.ui.theme.WatchRSSTheme

class ProfileActivity : BaseHeytapActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()

        setContent {
            WatchRSSTheme {
                ProfileScreen(
                    onAccountClick = {
                        HeyToast.showToast(this, "暂未接入欢太账号", android.widget.Toast.LENGTH_SHORT)
                    },
                    onFavoritesClick = {
                        if (!allowNavigation()) return@ProfileScreen
                        val intent = Intent(this, SavedItemsActivity::class.java)
                        intent.putExtra(
                            SavedItemsActivity.EXTRA_SAVE_TYPE,
                            com.lightningstudio.watchrss.data.rss.SaveType.FAVORITE.name
                        )
                        startActivity(intent)
                    },
                    onWatchLaterClick = {
                        if (!allowNavigation()) return@ProfileScreen
                        val intent = Intent(this, SavedItemsActivity::class.java)
                        intent.putExtra(
                            SavedItemsActivity.EXTRA_SAVE_TYPE,
                            com.lightningstudio.watchrss.data.rss.SaveType.WATCH_LATER.name
                        )
                        startActivity(intent)
                    },
                    onSettingsClick = {
                        if (!allowNavigation()) return@ProfileScreen
                        startActivity(Intent(this, SettingsActivity::class.java))
                    },
                    onAboutClick = {
                        if (!allowNavigation()) return@ProfileScreen
                        startActivity(Intent(this, AboutActivity::class.java))
                    }
                )
            }
        }
    }
}
