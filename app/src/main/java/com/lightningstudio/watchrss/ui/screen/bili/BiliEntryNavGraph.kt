package com.lightningstudio.watchrss.ui.screen.bili

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.heytap.wearable.support.widget.HeyToast
import com.lightningstudio.watchrss.BiliChannelInfoActivity
import com.lightningstudio.watchrss.BiliDetailActivity
import com.lightningstudio.watchrss.BiliLoginActivity
import com.lightningstudio.watchrss.data.bili.BiliRepository
import com.lightningstudio.watchrss.ui.viewmodel.BiliFeedViewModel
import com.lightningstudio.watchrss.ui.viewmodel.BiliViewModelFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

object BiliRoutes {
    const val FEED = "bili_feed"
}

@Composable
fun BiliEntryNavGraph(repository: BiliRepository) {
    val navController = rememberNavController()
    val factory = remember(repository) { BiliViewModelFactory(repository) }
    val context = androidx.compose.ui.platform.LocalContext.current

    NavHost(
        navController = navController,
        startDestination = BiliRoutes.FEED
    ) {
        composable(BiliRoutes.FEED) {
            val viewModel: BiliFeedViewModel = viewModel(factory = factory)
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

            BiliFeedScreen(
                uiState = uiState,
                onLoginClick = { context.startActivity(BiliLoginActivity.createIntent(context)) },
                onRefresh = viewModel::refresh,
                onHeaderClick = { context.startActivity(BiliChannelInfoActivity.createIntent(context)) },
                onItemClick = { item ->
                    context.startActivity(
                        BiliDetailActivity.createIntent(context, item.aid, item.bvid, item.cid)
                    )
                }
            )
        }
    }
}
