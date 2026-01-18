package com.lightningstudio.watchrss

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import com.lightningstudio.watchrss.ui.widget.ProgressRingView
import java.io.File

class WebViewActivity : BaseHeytapActivity() {
    private lateinit var webView: WebView
    private lateinit var loadingRing: ProgressRingView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemBars()
        setContentView(R.layout.activity_web_view)

        webView = findViewById(R.id.web_view)
        loadingRing = findViewById(R.id.web_loading_ring)
        loadingRing.setShowBase(false)

        val url = intent.getStringExtra(EXTRA_URL)
        if (url.isNullOrBlank()) {
            finish()
            return
        }

        setupWebView()
        onBackPressedDispatcher.addCallback(this) {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                finish()
            }
        }
        webView.loadUrl(url)
    }

    private fun setupWebView() {
        webView.setBackgroundColor(0xFF000000.toInt())
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            allowFileAccess = true
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                loadingRing.visibility = View.VISIBLE
                loadingRing.setProgress(0f)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                loadingRing.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView?,
                request: android.webkit.WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                loadingRing.visibility = View.GONE
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                val clamped = (newProgress / 100f).coerceIn(0f, 1f)
                loadingRing.setProgress(clamped)
                if (newProgress >= 100) {
                    loadingRing.visibility = View.GONE
                } else {
                    loadingRing.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_URL = "url"

        fun createIntent(context: Context, link: String): Intent {
            val trimmed = link.trim()
            val resolved = if (trimmed.startsWith("/")) {
                Uri.fromFile(File(trimmed)).toString()
            } else {
                trimmed
            }
            return Intent(context, WebViewActivity::class.java).putExtra(EXTRA_URL, resolved)
        }
    }
}
