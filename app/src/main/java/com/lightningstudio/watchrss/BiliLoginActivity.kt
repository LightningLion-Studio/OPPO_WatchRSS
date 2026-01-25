package com.lightningstudio.watchrss

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.heytap.wearable.support.widget.HeyToast
import com.lightningstudio.watchrss.ui.screen.bili.BiliLoginScreen
import com.lightningstudio.watchrss.ui.theme.WatchRSSTheme
import com.lightningstudio.watchrss.ui.viewmodel.BiliLoginViewModel
import com.lightningstudio.watchrss.ui.viewmodel.BiliViewModelFactory

class BiliLoginActivity : BaseHeytapActivity() {
    private val repository by lazy { (application as WatchRssApplication).container.biliRepository }
    private val viewModel: BiliLoginViewModel by viewModels {
        BiliViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()

        setContent {
            WatchRSSTheme {
                val context = LocalContext.current
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.startLogin()
                }
                LaunchedEffect(uiState.message) {
                    val message = uiState.message
                    if (!message.isNullOrBlank()) {
                        HeyToast.showToast(context, message, android.widget.Toast.LENGTH_SHORT)
                        viewModel.clearMessage()
                    }
                }

                BiliLoginScreen(
                    uiState = uiState,
                    onRefreshQr = viewModel::startLogin,
                    onLoginSuccess = {
                        setResult(RESULT_OK)
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, BiliLoginActivity::class.java)
        }
    }
}
