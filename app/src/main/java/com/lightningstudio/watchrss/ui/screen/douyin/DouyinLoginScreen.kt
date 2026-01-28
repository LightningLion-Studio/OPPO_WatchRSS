package com.lightningstudio.watchrss.ui.screen.douyin

import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

@Composable
fun DouyinLoginScreen(
    onLoginComplete: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var cookieResult by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var loginPanelExists by remember { mutableStateOf(false) }
    var loginPanelWasVisible by remember { mutableStateOf(false) }

    // Smooth progress animation
    val animatedProgress by animateFloatAsState(
        targetValue = loadProgress,
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = "progress"
    )

    // Check for login panel every 56ms
    LaunchedEffect(webViewRef) {
        val webView = webViewRef ?: return@LaunchedEffect
        while (cookieResult == null && errorMessage == null) {
            delay(56)
            webView.evaluateJavascript(LOGIN_PANEL_CHECK_SCRIPT) { result ->
                val exists = result == "true"
                if (loginPanelWasVisible && !exists) {
                    // Login panel disappeared - login successful
                    val cookieManager = CookieManager.getInstance()
                    val allCookies = cookieManager.getCookie("https://www.douyin.com") ?: ""
                    val douyinCookies = allCookies.split(";")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .joinToString("; ")
                    cookieResult = douyinCookies
                    onLoginComplete(douyinCookies)
                }
                loginPanelWasVisible = exists
                loginPanelExists = exists
            }
        }
    }

    // Clean page every 1 second
    LaunchedEffect(webViewRef) {
        val webView = webViewRef ?: return@LaunchedEffect
        while (cookieResult == null && errorMessage == null) {
            delay(1000)
            webView.evaluateJavascript(CLEAN_DOUYIN_CHAT_SCRIPT, null)
            webView.evaluateJavascript(CLEAN_SVG_SCRIPT, null)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            cookieResult != null -> {
                // Show cookie result
                CookieResultView(
                    cookies = cookieResult!!,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(cookieResult!!))
                    }
                )
            }
            errorMessage != null -> {
                // Show error page
                ErrorView(errorMessage = errorMessage!!)
            }
            else -> {
                // Show WebView
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    userAgentString = USER_AGENT
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        return false
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        super.onReceivedError(view, request, error)
                                        if (request?.isForMainFrame == true) {
                                            errorMessage = error?.description?.toString() ?: "加载失败"
                                            isLoading = false
                                        }
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

                                loadUrl(LOGIN_URL)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { webViewRef = it }
                    )
                }

                // Circular loading indicator
                if (isLoading) {
                    CircularLoadingIndicator(progress = animatedProgress)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
        }
    }
}

@Composable
private fun CircularLoadingIndicator(progress: Float) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(116.dp)
        ) {
            val strokeWidth = 4.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val centerX = size.width / 2
            val centerY = size.height / 2

            // Background circle
            drawCircle(
                color = Color.Gray.copy(alpha = 0.3f),
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = strokeWidth)
            )

            // Progress arc
            if (progress > 0f) {
                drawArc(
                    color = Color(0xFF1E88E5),
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(
                        centerX - radius,
                        centerY - radius
                    ),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round
                    )
                )
            }
        }
    }
}

@Composable
private fun ErrorView(errorMessage: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                tint = Color.Red,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "加载失败",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun CookieResultView(cookies: String, onCopy: () -> Unit) {
    val scrollState = rememberScrollState()
    var showCopyHint by remember { mutableStateOf(false) }

    LaunchedEffect(showCopyHint) {
        if (showCopyHint) {
            delay(2000)
            showCopyHint = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .verticalScroll(scrollState)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            onCopy()
                            showCopyHint = true
                        }
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "登录成功",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Cookie:",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = cookies,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "长按复制",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        if (showCopyHint) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color.DarkGray, shape = MaterialTheme.shapes.small)
                    .padding(12.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Text(
                    text = "已复制到剪贴板",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

private const val LOGIN_URL = "https://www.douyin.com/chat?isPopup=1"
private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36 Edg/144.0.0.0"

private const val LOGIN_PANEL_CHECK_SCRIPT = """
(function() {
  const loginPanel = document.getElementById("login-panel-new");
  const isExists = !!loginPanel;
  if (isExists) {
    console.log('✅ 存在<div id="login-panel-new">元素');
    return true;
  } else {
    console.log('❌ 不存在<div id="login-panel-new">元素');
    return false;
  }
})();
"""

private const val CLEAN_DOUYIN_CHAT_SCRIPT = """
(function() {
  const targetDivs = Array.from(document.querySelectorAll("div")).filter(
    (div) => div.textContent.trim() === "抖音聊天"
  );
  if (targetDivs.length === 0) return;

  function getDomDepth(element) {
    let depth = 0;
    let current = element;
    while (current.parentNode && current.parentNode !== document) {
      depth++;
      current = current.parentNode;
    }
    return depth;
  }

  let deepestDiv = targetDivs[0];
  let maxDepth = getDomDepth(deepestDiv);
  targetDivs.forEach((div) => {
    const currentDepth = getDomDepth(div);
    if (currentDepth > maxDepth) {
      maxDepth = currentDepth;
      deepestDiv = div;
    }
  });

  deepestDiv.remove();
})();
"""

private const val CLEAN_SVG_SCRIPT = """
(function() {
  const targetSvgs = Array.from(document.querySelectorAll("svg")).filter(
    (svg) => {
      return (
        svg.getAttribute("xmlns") === "http://www.w3.org/2000/svg" &&
        svg.getAttribute("width") === "37" &&
        svg.getAttribute("height") === "36" &&
        svg.getAttribute("viewBox") === "0 0 37 36" &&
        svg.getAttribute("fill") === "none"
      );
    }
  );

  targetSvgs.forEach((svg) => {
    if (svg && svg.parentNode) {
      svg.parentNode.remove();
    }
  });
})();
"""

