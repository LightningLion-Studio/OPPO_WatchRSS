package com.lightningstudio.watchrss

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.heytap.wearable.support.widget.HeyToast
import com.lightningstudio.watchrss.ui.screen.bili.BiliSettingsScreen
import com.lightningstudio.watchrss.ui.theme.WatchRSSTheme
import com.lightningstudio.watchrss.ui.viewmodel.BiliSettingsViewModel
import com.lightningstudio.watchrss.ui.viewmodel.BiliViewModelFactory

class BiliSettingsActivity : BaseHeytapActivity() {
    private val repository by lazy { (application as WatchRssApplication).container.biliRepository }
    private val viewModel: BiliSettingsViewModel by viewModels {
        BiliViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()

        setContent {
            WatchRSSTheme {
                val context = LocalContext.current
                val uiState by viewModel.uiState.collectAsState()
                val lifecycleOwner = LocalLifecycleOwner.current

                LaunchedEffect(Unit) {
                    viewModel.refreshLoginState()
                }
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.refreshLoginState()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }
                LaunchedEffect(uiState.message) {
                    val message = uiState.message
                    if (!message.isNullOrBlank()) {
                        HeyToast.showToast(context, message, android.widget.Toast.LENGTH_SHORT)
                        viewModel.clearMessage()
                    }
                }

                BiliSettingsScreen(
                    isLoggedIn = uiState.isLoggedIn,
                    onLogout = viewModel::logout
                )
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, BiliSettingsActivity::class.java)
        }
    }
}
