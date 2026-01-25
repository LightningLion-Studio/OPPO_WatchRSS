package com.lightningstudio.watchrss.ui.screen.bili

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.heytap.wearable.support.widget.HeyToast
import com.lightningstudio.watchrss.WebViewActivity
import com.lightningstudio.watchrss.data.bili.BiliRepository
import com.lightningstudio.watchrss.ui.screen.WebViewLoginScreen
import com.lightningstudio.watchrss.ui.viewmodel.BiliDetailViewModel
import com.lightningstudio.watchrss.ui.viewmodel.BiliFeedViewModel
import com.lightningstudio.watchrss.ui.viewmodel.BiliListType
import com.lightningstudio.watchrss.ui.viewmodel.BiliListViewModel
import com.lightningstudio.watchrss.ui.viewmodel.BiliLoginViewModel
import com.lightningstudio.watchrss.ui.viewmodel.BiliPlayerViewModel
import com.lightningstudio.watchrss.ui.viewmodel.BiliViewModelFactory
import kotlinx.coroutines.launch

object BiliRoutes {
    const val FEED = "bili_feed"
    const val LOGIN = "bili_login"
    const val WEB_LOGIN = "bili_web_login"
    const val DETAIL = "bili_detail?aid={aid}&bvid={bvid}&cid={cid}"
    const val PLAYER = "bili_player?aid={aid}&bvid={bvid}&cid={cid}"
    const val LIST = "bili_list/{type}"

    fun detail(aid: Long?, bvid: String?, cid: Long?): String {
        return "bili_detail?aid=${aid ?: ""}&bvid=${bvid ?: ""}&cid=${cid ?: ""}"
    }

    fun player(aid: Long?, bvid: String?, cid: Long?): String {
        return "bili_player?aid=${aid ?: ""}&bvid=${bvid ?: ""}&cid=${cid ?: ""}"
    }

    fun list(type: BiliListType): String = "bili_list/${type.id}"
}

@Composable
fun BiliEntryNavGraph(repository: BiliRepository) {
    val navController = rememberNavController()
    val factory = remember(repository) { BiliViewModelFactory(repository) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

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
                onOpenWatchLater = { navController.navigate(BiliRoutes.list(BiliListType.WATCH_LATER)) },
                onOpenHistory = { navController.navigate(BiliRoutes.list(BiliListType.HISTORY)) },
                onOpenFavorites = { navController.navigate(BiliRoutes.list(BiliListType.FAVORITE)) },
                onItemClick = { item ->
                    navController.navigate(BiliRoutes.detail(item.aid, item.bvid, item.cid))
                }
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
            route = BiliRoutes.DETAIL,
            arguments = listOf(
                navArgument("aid") { type = NavType.StringType; defaultValue = "" },
                navArgument("bvid") { type = NavType.StringType; defaultValue = "" },
                navArgument("cid") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val viewModel: BiliDetailViewModel = viewModel(backStackEntry, factory = factory)
            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(uiState.message) {
                val message = uiState.message
                if (!message.isNullOrBlank()) {
                    HeyToast.showToast(context, message, android.widget.Toast.LENGTH_SHORT)
                    viewModel.clearMessage()
                }
            }

            BiliDetailScreen(
                uiState = uiState,
                onPlayClick = {
                    val cid = viewModel.selectedCid()
                    navController.navigate(BiliRoutes.player(uiState.detail?.item?.aid, uiState.detail?.item?.bvid, cid))
                },
                onSelectPage = viewModel::selectPage,
                onLike = viewModel::like,
                onCoin = viewModel::coin,
                onFavorite = viewModel::favorite,
                onWatchLater = viewModel::addToWatchLater,
                onShare = {
                    val link = repository.shareLink(
                        uiState.detail?.item?.bvid,
                        uiState.detail?.item?.aid
                    )
                    shareLink(context, uiState.detail?.item?.title, link)
                }
            )
        }
        composable(
            route = BiliRoutes.PLAYER,
            arguments = listOf(
                navArgument("aid") { type = NavType.StringType; defaultValue = "" },
                navArgument("bvid") { type = NavType.StringType; defaultValue = "" },
                navArgument("cid") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val viewModel: BiliPlayerViewModel = viewModel(backStackEntry, factory = factory)
            val uiState by viewModel.uiState.collectAsState()
            val link = repository.shareLink(
                backStackEntry.arguments?.getString("bvid"),
                backStackEntry.arguments?.getString("aid")?.toLongOrNull()
            )

            BiliPlayerScreen(
                uiState = uiState,
                onRetry = viewModel::loadPlayUrl,
                onOpenWeb = {
                    val safeLink = link ?: return@BiliPlayerScreen
                    context.startActivity(WebViewActivity.createIntent(context, safeLink))
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
                    navController.navigate(BiliRoutes.detail(item.aid, item.bvid, item.cid))
                }
            )
        }
    }
}

private fun shareLink(context: Context, title: String?, link: String?) {
    val safeTitle = title?.trim().orEmpty()
    val safeLink = link?.trim().orEmpty()
    if (safeTitle.isEmpty() && safeLink.isEmpty()) return
    val text = when {
        safeTitle.isNotEmpty() && safeLink.isNotEmpty() -> "$safeTitle\n$safeLink"
        safeTitle.isNotEmpty() -> safeTitle
        else -> safeLink
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "分享"))
}
