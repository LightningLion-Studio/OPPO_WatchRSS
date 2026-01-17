package com.lightningstudio.watchrss.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lightningstudio.watchrss.WatchRssApplication
import com.lightningstudio.watchrss.ui.screen.rss.AddRssScreen
import com.lightningstudio.watchrss.ui.screen.rss.DetailScreen
import com.lightningstudio.watchrss.ui.screen.rss.FeedScreen
import com.lightningstudio.watchrss.ui.screen.rss.HomeScreen
import com.lightningstudio.watchrss.ui.screen.rss.SettingsScreen
import com.lightningstudio.watchrss.ui.viewmodel.AddRssViewModel
import com.lightningstudio.watchrss.ui.viewmodel.AppViewModelFactory
import com.lightningstudio.watchrss.ui.viewmodel.DetailViewModel
import com.lightningstudio.watchrss.ui.viewmodel.FeedViewModel
import com.lightningstudio.watchrss.ui.viewmodel.HomeViewModel
import com.lightningstudio.watchrss.ui.viewmodel.SettingsViewModel

object Routes {
    const val HOME = "home"
    const val ADD_RSS = "add_rss"
    const val FEED = "feed/{channelId}"
    const val DETAIL = "detail/{itemId}"
    const val SETTINGS = "settings"

    fun feed(channelId: Long) = "feed/$channelId"
    fun detail(itemId: Long) = "detail/$itemId"
}

@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val container = (context.applicationContext as WatchRssApplication).container
    val factory = remember(container) { AppViewModelFactory(container) }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {
        composable(Routes.HOME) {
            val viewModel: HomeViewModel = viewModel(factory = factory)
            HomeScreen(
                channels = viewModel.channels,
                isRefreshing = viewModel.isRefreshing,
                message = viewModel.message,
                onRefresh = viewModel::refresh,
                onMessageShown = viewModel::clearMessage,
                onAddRss = { navController.navigate(Routes.ADD_RSS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onChannelClick = { channelId ->
                    navController.navigate(Routes.feed(channelId))
                }
            )
        }
        composable(Routes.ADD_RSS) {
            val viewModel: AddRssViewModel = viewModel(factory = factory)
            AddRssScreen(
                uiState = viewModel.uiState,
                onUrlChange = viewModel::updateUrl,
                onSubmit = viewModel::submit,
                onBack = { navController.popBackStack() },
                onChannelAdded = { channelId ->
                    navController.navigate(Routes.feed(channelId)) {
                        popUpTo(Routes.HOME)
                    }
                },
                onConsumed = viewModel::consumeCreatedChannel
            )
        }
        composable(
            route = Routes.FEED,
            arguments = listOf(navArgument("channelId") { type = NavType.LongType })
        ) { backStackEntry ->
            val viewModel: FeedViewModel = viewModel(backStackEntry, factory = factory)
            FeedScreen(
                channel = viewModel.channel,
                items = viewModel.items,
                message = viewModel.message,
                onMessageShown = viewModel::clearMessage,
                onBack = { navController.popBackStack() },
                onRefresh = viewModel::refresh,
                onItemClick = { itemId ->
                    navController.navigate(Routes.detail(itemId))
                }
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("itemId") { type = NavType.LongType })
        ) { backStackEntry ->
            val viewModel: DetailViewModel = viewModel(backStackEntry, factory = factory)
            DetailScreen(
                item = viewModel.item,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            val viewModel: SettingsViewModel = viewModel(factory = factory)
            SettingsScreen(
                cacheLimitMb = viewModel.cacheLimitMb,
                cacheUsageMb = viewModel.cacheUsageMb,
                onBack = { navController.popBackStack() },
                onSelectCacheLimit = viewModel::updateCacheLimitMb
            )
        }
    }
}
