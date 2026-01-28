package com.lightningstudio.watchrss

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.lightningstudio.watchrss.ui.screen.WebViewLoginScreen
import com.lightningstudio.watchrss.ui.screen.douyin.DouyinFeedScreen
import com.lightningstudio.watchrss.ui.theme.WatchRSSTheme
import com.lightningstudio.watchrss.ui.viewmodel.DouyinFeedViewModel
import com.lightningstudio.watchrss.ui.viewmodel.DouyinViewModelFactory

class DouyinEntryActivity : BaseHeytapActivity() {
    private val repository by lazy { (application as WatchRssApplication).container.douyinRepository }
    private val viewModel: DouyinFeedViewModel by viewModels {
        DouyinViewModelFactory(repository)
    }
    private var disableSwipeBack = false

    private val loginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val cookies = result.data?.getStringExtra(DouyinLoginActivity.EXTRA_COOKIES)
            if (!cookies.isNullOrEmpty()) {
                viewModel.applyCookie(cookies)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()

        setContent {
            WatchRSSTheme {
                val baseDensity = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(2f, baseDensity.fontScale)) {
                    val uiState by viewModel.uiState.collectAsState()
                    if (!uiState.isLoggedIn) {
                        SideEffect { disableSwipeBack = true }
                        WebViewLoginScreen(
                            loginUrl = "https://www.douyin.com/user/self",
                            cookieDomain = "https://www.douyin.com",
                            onLoginComplete = viewModel::applyCookie,
                            onBack = { }
                        )
                    } else {
                        SideEffect { disableSwipeBack = false }
                        DouyinFeedScreen(
                            uiState = uiState,
                            onRefresh = viewModel::refresh,
                            onItemClick = { _, index ->
                                val items = uiState.items
                                if (items.isEmpty()) return@DouyinFeedScreen
                                startActivity(DouyinPlayerActivity.createIntent(this, items, index))
                            },
                            onLoginClick = {
                                loginLauncher.launch(DouyinLoginActivity.createIntent(this))
                            }
                        )
                    }
                }
            }
        }
    }

    override fun isSwipeBackEnabled(): Boolean = !disableSwipeBack
}
