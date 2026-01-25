package com.lightningstudio.watchrss.ui.screen.bili

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.heytap.wearable.support.widget.HeyToast
import com.lightningstudio.watchrss.BiliDetailActivity
import com.lightningstudio.watchrss.data.bili.BiliRepository
import com.lightningstudio.watchrss.ui.screen.WebViewLoginScreen
import com.lightningstudio.watchrss.ui.viewmodel.BiliFeedViewModel
import com.lightningstudio.watchrss.ui.viewmodel.BiliListType
import com.lightningstudio.watchrss.ui.viewmodel.BiliListViewModel
import com.lightningstudio.watchrss.ui.viewmodel.BiliLoginViewModel
import com.lightningstudio.watchrss.ui.viewmodel.BiliViewModelFactory
import kotlinx.coroutines.launch

object BiliRoutes {
    const val FEED = "bili_feed"
    const val LOGIN = "bili_login"
    const val WEB_LOGIN = "bili_web_login"
    const val CHANNEL_INFO = "bili_channel_info"
    const val LIST = "bili_list/{type}"

    fun list(type: BiliListType): String = "bili_list/${type.id}"
}

@Composable
fun BiliEntryNavGraph(repository: BiliRepository) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val factory = remember(repository) { BiliViewModelFactory(repository) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val rootView = LocalView.current
    val scope = rememberCoroutineScope()
    val resetRootView: () -> Unit = {
        rootView.rootView.animate().cancel()
        rootView.rootView.translationX = 0f
        rootView.rootView.translationY = 0f
    }
    LaunchedEffect(navBackStackEntry?.destination?.route) {
        resetRootView()
    }

    NavHost(
        navController = navController,
        startDestination = BiliRoutes.FEED
    ) {
        composable(BiliRoutes.FEED) {
            val viewModel: BiliFeedViewModel = viewModel(factory = factory)
            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(Unit) {
                viewModel.refreshLoginState()
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
                onLoginClick = { navController.navigate(BiliRoutes.LOGIN) },
                onRefresh = viewModel::refresh,
                onHeaderClick = { navController.navigate(BiliRoutes.CHANNEL_INFO) },
                onItemClick = { item ->
                    context.startActivity(
                        BiliDetailActivity.createIntent(context, item.aid, item.bvid, item.cid)
                    )
                }
            )
        }
        composable(BiliRoutes.CHANNEL_INFO) {
            val feedBackStackEntry = remember(navController) { navController.getBackStackEntry(BiliRoutes.FEED) }
            val feedViewModel: BiliFeedViewModel = viewModel(feedBackStackEntry, factory = factory)
            val uiState by feedViewModel.uiState.collectAsState()

            LaunchedEffect(Unit) {
                feedViewModel.refreshLoginState()
            }

            BiliChannelInfoScreen(
                isLoggedIn = uiState.isLoggedIn,
                lastRefreshAt = uiState.lastRefreshAt,
                onLoginClick = { navController.navigate(BiliRoutes.LOGIN) },
                onOpenWatchLater = { navController.navigate(BiliRoutes.list(BiliListType.WATCH_LATER)) },
                onOpenHistory = { navController.navigate(BiliRoutes.list(BiliListType.HISTORY)) },
                onOpenFavorites = { navController.navigate(BiliRoutes.list(BiliListType.FAVORITE)) }
            )
        }
        composable(BiliRoutes.LOGIN) {
            val viewModel: BiliLoginViewModel = viewModel(factory = factory)
            val feedBackStackEntry = remember(navController) { navController.getBackStackEntry(BiliRoutes.FEED) }
            val feedViewModel: BiliFeedViewModel = viewModel(feedBackStackEntry, factory = factory)
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
                    feedViewModel.refreshLoginState()
                    feedViewModel.refresh()
                    navController.popBackStack(BiliRoutes.FEED, inclusive = false)
                }
            )
        }
        composable(BiliRoutes.WEB_LOGIN) {
            val feedBackStackEntry = remember(navController) { navController.getBackStackEntry(BiliRoutes.FEED) }
            val feedViewModel: BiliFeedViewModel = viewModel(feedBackStackEntry, factory = factory)
            WebViewLoginScreen(
                loginUrl = "https://passport.bilibili.com/login",
                cookieDomain = "https://www.bilibili.com",
                onLoginComplete = { cookies ->
                    scope.launch {
                        val result = repository.applyCookieHeader(cookies)
                        if (result.isSuccess) {
                            HeyToast.showToast(context, "登录成功", android.widget.Toast.LENGTH_SHORT)
                            feedViewModel.refreshLoginState()
                            feedViewModel.refresh()
                            navController.popBackStack(BiliRoutes.FEED, inclusive = false)
                        } else {
                            HeyToast.showToast(
                                context,
                                result.exceptionOrNull()?.message ?: "登录失败",
                                android.widget.Toast.LENGTH_SHORT
                            )
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = BiliRoutes.LIST,
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { backStackEntry ->
            val viewModel: BiliListViewModel = viewModel(backStackEntry, factory = factory)
            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(uiState.message) {
                val message = uiState.message
                if (!message.isNullOrBlank()) {
                    HeyToast.showToast(context, message, android.widget.Toast.LENGTH_SHORT)
                    viewModel.clearMessage()
                }
            }

            BiliListScreen(
                uiState = uiState,
                onRefresh = viewModel::refresh,
                onLoadMore = viewModel::loadMore,
                onItemClick = { item ->
                    context.startActivity(
                        BiliDetailActivity.createIntent(context, item.aid, item.bvid, item.cid)
                    )
                }
            )
        }
    }
}
