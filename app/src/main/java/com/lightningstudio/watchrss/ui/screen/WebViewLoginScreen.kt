package com.lightningstudio.watchrss.ui.screen

import android.os.Message
import android.view.MotionEvent
import android.view.VelocityTracker
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.lightningstudio.watchrss.R
import com.lightningstudio.watchrss.ui.theme.WatchRSSTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun WebViewLoginScreen(
    loginUrl: String = "https://www.douyin.com/user/self",
    cookieDomain: String = "https://www.douyin.com",
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
    var webWindowBounds by remember { mutableStateOf<Rect?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val motionTracker = remember {
        object {
            var tracker: VelocityTracker? = null
            var lastY: Float = 0f
        }
    }

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
                    .pointerInteropFilter { event ->
                        val bounds = webWindowBounds
                        if (bounds == null) return@pointerInteropFilter false
                        val inVerticalRange = event.y in bounds.top..bounds.bottom
                        val isSideBlank = inVerticalRange &&
                            (event.x < bounds.left || event.x > bounds.right)
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                if (!isSideBlank) return@pointerInteropFilter false
                                motionTracker.tracker?.recycle()
                                motionTracker.tracker = VelocityTracker.obtain().apply {
                                    addMovement(event)
                                }
                                motionTracker.lastY = event.y
                                true
                            }
                            MotionEvent.ACTION_MOVE -> {
                                if (motionTracker.tracker == null) return@pointerInteropFilter false
                                motionTracker.tracker?.addMovement(event)
                                val dy = event.y - motionTracker.lastY
                                motionTracker.lastY = event.y
                                if (dy != 0f) {
                                    webViewRef?.scrollBy(0, (-dy).roundToInt())
                                }
                                true
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                val tracker = motionTracker.tracker ?: return@pointerInteropFilter false
                                tracker.addMovement(event)
                                tracker.computeCurrentVelocity(1000)
                                val velocityY = tracker.yVelocity
                                webViewRef?.flingScroll(0, (-velocityY).roundToInt())
                                tracker.recycle()
                                motionTracker.tracker = null
                                true
                            }
                            else -> motionTracker.tracker != null
                        }
                    }
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
                            modifier = Modifier.padding(12.dp)
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
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    // Show WebView scaled from 400x(16:9) into a 150x(16:9) window, offset upward by 14dp.
                    val targetWidth = 150.dp
                    val targetHeight = targetWidth * 16f / 9f
                    val webViewWidth = 400.dp
                    val webViewHeight = webViewWidth * 16f / 9f
                    val scale = targetWidth.value / webViewWidth.value

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .requiredSize(targetWidth, targetHeight)
                                .offset(y = (-14).dp)
                                .clipToBounds()
                                .onGloballyPositioned { coordinates ->
                                    webWindowBounds = coordinates.boundsInParent()
                                },
                            contentAlignment = Alignment.Center
                        ) {
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
                                            setSupportMultipleWindows(false)
                                            setSupportZoom(true)
                                            builtInZoomControls = true
                                            displayZoomControls = false
                                        }
                                        scaleX = scale
                                        scaleY = scale
                                        post {
                                            pivotX = width / 2f
                                            pivotY = height / 2f
                                        }
                                        setTag(R.id.tag_skip_scale_reset, true)
                                        webViewClient = object : WebViewClient() {
                                            override fun shouldOverrideUrlLoading(
                                                view: WebView?,
                                                request: WebResourceRequest?
                                            ): Boolean {
                                                val url = request?.url?.toString().orEmpty()
                                                return if (url.startsWith("http")) {
                                                    view?.loadUrl(url)
                                                    true
                                                } else {
                                                    true
                                                }
                                            }

                                            @Suppress("DEPRECATION")
                                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                                val safeUrl = url.orEmpty()
                                                return if (safeUrl.startsWith("http")) {
                                                    view?.loadUrl(safeUrl)
                                                    true
                                                } else {
                                                    true
                                                }
                                            }

                                            override fun onPageFinished(view: WebView?, url: String?) {
                                                super.onPageFinished(view, url)
                                                view?.scaleX = scale
                                                view?.scaleY = scale
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

                                            override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
                                                // Ignore full-screen requests to keep the WebView in-place.
                                                callback?.onCustomViewHidden()
                                            }

                                            override fun onCreateWindow(
                                                view: WebView?,
                                                isDialog: Boolean,
                                                isUserGesture: Boolean,
                                                resultMsg: Message?
                                            ): Boolean {
                                                val transport = resultMsg?.obj as? WebView.WebViewTransport
                                                if (transport != null && view != null) {
                                                    transport.webView = view
                                                    resultMsg.sendToTarget()
                                                    return true
                                                }
                                                return false
                                            }
                                        }

                                        loadUrl(loginUrl)
                                    }
                                },
                                modifier = Modifier
                                    .requiredWidth(webViewWidth)
                                    .requiredHeight(webViewHeight),
                                update = { webViewRef = it }
                            )
                        }
                    }
                }
            }

            // Progress bar at top
            if (isLoading && !showSuccess) {
                LinearProgressIndicator(
                    progress = { loadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
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
                        strokeWidth = 2.dp
                    )
                }
            }

            // Login complete button
            if (showLoginButton && !showSuccess) {
                Button(
                    onClick = { showConfirmDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 18.dp)
                        .fillMaxWidth(0.7f)
                        .height(36.dp),
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
                        // Get cookies for login domain
                        val cookieManager = CookieManager.getInstance()
                        val cookies = cookieManager.getCookie(cookieDomain)

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
                loginUrl = "https://www.douyin.com/user/self",
                cookieDomain = "https://www.douyin.com",
                onLoginComplete = {},
                onBack = {}
            )
        }
    }
}
