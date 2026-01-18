package com.lightningstudio.watchrss.ui.screen

import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.lightningstudio.watchrss.ui.theme.WatchRSSTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WebViewLoginScreen(
    onLoginComplete: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showLoginButton by remember { mutableStateOf(false) }
    var webViewInitialized by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Timer to show login button after 5 seconds
    LaunchedEffect(webViewInitialized) {
        if (webViewInitialized && !showSuccess) {
            delay(5000)
            showLoginButton = true
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val bgOuter1 = Color(0xFF0B1211)
        val bgOuter2 = Color(0xFF070B0B)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(bgOuter1, bgOuter2)
                    )
                )
        ) {
            // Inscribed square (466x466 circle, square fits inside)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .align(Alignment.Center)
                    .background(Color.Black)
            ) {
                if (showSuccess) {
                    // Show success message
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓ 您已登录完成",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(11.65.dp)
                        )
                    }
                } else if (errorMessage != null) {
                    // Show error message
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "加载失败: $errorMessage",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(11.65.dp)
                        )
                    }
                } else {
                    // Show WebView
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36 Edg/137.0.0.0" // 为了打开电脑版网页
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false
                                        if (!showSuccess) {
                                            webViewInitialized = true
                                        }
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        super.onReceivedError(view, request, error)
                                        errorMessage = error?.description?.toString() ?: "加载失败"
                                        isLoading = false
                                    }
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        loadProgress = newProgress / 100f
                                        if (newProgress == 100) {
                                            isLoading = false
                                        }
                                    }
                                }

                                loadUrl("https://www.douyin.com/user/self")
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Progress bar at top
            if (isLoading && !showSuccess) {
                LinearProgressIndicator(
                    progress = { loadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.913.dp)
                        .align(Alignment.TopCenter),
                    color = Color(0xFF1E88E5),
                    trackColor = Color.Transparent
                )
            }

            // Loading indicator
            if (isLoading && !showSuccess) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF1E88E5),
                        strokeWidth = 2.184.dp
                    )
                }
            }

            // Login complete button
            if (showLoginButton && !showSuccess) {
                Button(
                    onClick = { showConfirmDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 17.475.dp)
                        .fillMaxWidth(0.7f)
                        .height(34.95.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E88E5)
                    )
                ) {
                    Text(
                        text = "我已登录完成",
                        color = Color.White
                    )
                }
            }
        }
    }

    // Confirmation Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
            },
            title = {
                Text(text = "确认登录")
            },
            text = {
                Text(text = "您确定已经完成登录了吗？")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Get cookies for .douyin.com domain
                        val cookieManager = CookieManager.getInstance()
                        val cookies = cookieManager.getCookie("douyin.com")

                        // Destroy WebView by showing success message
                        showSuccess = true
                        showLoginButton = false

                        // Pass cookies to parent component
                        onLoginComplete(cookies ?: "")

                        // Close after 2 seconds
                        coroutineScope.launch {
                            delay(2000)
                            onBack()
                        }
                        showConfirmDialog = false
                    }
                ) {
                    Text(text = "确认", color = Color(0xFF1E88E5))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                    }
                ) {
                    Text(text = "取消")
                }
            }
        )
    }
}

@Preview(widthDp = 466, heightDp = 466, showBackground = true)
@Composable
private fun WebViewLoginScreenPreview() {
    WatchRSSTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            WebViewLoginScreen(
                onLoginComplete = {},
                onBack = {}
            )
        }
    }
}
