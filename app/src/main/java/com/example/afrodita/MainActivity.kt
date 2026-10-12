package com.example.afrodita

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    private var webView: WebView? = null
    private var pendingDeepLinkUrl: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Capture initial deep link intent if launched from email (Cold Start)
        handleIntent(intent)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                webView = this
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = true
                                settings.allowContentAccess = true
                                settings.javaScriptCanOpenWindowsAutomatically = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                                webChromeClient = object : WebChromeClient() {
                                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                        Log.d(
                                            "AfroditaWebView",
                                            "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}"
                                        )
                                        return super.onConsoleMessage(consoleMessage)
                                    }
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        pendingDeepLinkUrl?.let { deepLink ->
                                            sendDeepLinkToJs(deepLink)
                                            pendingDeepLinkUrl = null
                                        }
                                    }
                                }
                                loadUrl("file:///android_asset/index.html")
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val action = intent?.action
        val dataStr = intent?.dataString
        if (Intent.ACTION_VIEW == action && dataStr != null) {
            if (webView != null) {
                sendDeepLinkToJs(dataStr)
            } else {
                pendingDeepLinkUrl = dataStr
            }
        }
    }

    private fun sendDeepLinkToJs(url: String) {
        try {
            val uri = Uri.parse(url)
            if (uri.scheme == "afrodita" && uri.host == "auth" && uri.path == "/callback") {
                val quotedUrl = JSONObject.quote(url)
                webView?.post {
                    webView?.evaluateJavascript(
                        "if(typeof handleDeepLink === 'function'){ handleDeepLink($quotedUrl); }",
                        null
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("AfroditaWebView", "Error enviando deep link a JS: ${e.message}")
        }
    }
}
