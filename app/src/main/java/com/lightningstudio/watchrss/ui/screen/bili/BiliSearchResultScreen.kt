package com.lightningstudio.watchrss.ui.screen.bili

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.lightningstudio.watchrss.sdk.bili.BiliSearchResultItem
import com.lightningstudio.watchrss.ui.components.EmptySearchState
import com.lightningstudio.watchrss.ui.components.LoadingIndicator
import com.lightningstudio.watchrss.ui.components.MediaSearchCard
import com.lightningstudio.watchrss.ui.components.PullRefreshBox
import com.lightningstudio.watchrss.ui.components.UserSearchCard
import com.lightningstudio.watchrss.ui.components.VideoSearchCard
import com.lightningstudio.watchrss.ui.viewmodel.BiliSearchResultViewModel
import com.lightningstudio.watchrss.ui.viewmodel.BiliViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiliSearchResultScreen(
    keyword: String,
    factory: BiliViewModelFactory,
    onNavigateBack: () -> Unit,
    onVideoClick: (Long?, String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BiliSearchResultViewModel = viewModel(factory = factory)
) {
    val searchResults = viewModel.searchResultFlow.collectAsLazyPagingItems()
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(keyword) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        PullRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    searchResults.refresh()
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                searchResults.loadState.refresh is LoadState.Loading -> {
                    LoadingIndicator(searchResults.loadState.refresh)
                }
                searchResults.loadState.refresh is LoadState.Error -> {
                    LoadingIndicator(searchResults.loadState.refresh)
                }
                searchResults.itemCount == 0 -> {
                    EmptySearchState()
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(searchResults.itemCount) { index ->
                            val item = searchResults[index]
                            when (item) {
                                is BiliSearchResultItem.Video -> {
                                    VideoSearchCard(
                                        video = item.data,
                                        onClick = { onVideoClick(item.data.aid, item.data.bvid) }
                                    )
                                }
                                is BiliSearchResultItem.User -> {
                                    UserSearchCard(
                                        user = item.data,
                                        onClick = { /* Handle user click */ }
                                    )
                                }
                                is BiliSearchResultItem.Media -> {
                                    MediaSearchCard(
                                        media = item.data,
                                        onClick = { /* Handle media click */ }
                                    )
                                }
                                null -> {}
                            }
                        }

                        item {
                            LoadingIndicator(searchResults.loadState.append)
                        }
                    }
                }
            }
        }
    }
}

