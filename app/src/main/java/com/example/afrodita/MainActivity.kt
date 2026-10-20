package com.example.afrodita

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    private var webView: WebView? = null
    private var pendingDeepLinkUrl: String? = null

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (filePathCallback != null) {
            val intentData = result.data
            val results: Array<Uri>? = if (result.resultCode == RESULT_OK && intentData != null) {
                val dataString = intentData.dataString
                if (dataString != null) {
                    arrayOf(Uri.parse(dataString))
                } else if (intentData.clipData != null) {
                    val count = intentData.clipData!!.itemCount
                    val uris = mutableListOf<Uri>()
                    for (i in 0 until count) {
                        uris.add(intentData.clipData!!.getItemAt(i).uri)
                    }
                    uris.toTypedArray()
                } else null
            } else null

            filePathCallback?.onReceiveValue(results)
            filePathCallback = null
        } else {
            filePathCallback?.onReceiveValue(null)
            filePathCallback = null
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNativePermissions()
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
                                settings.databaseEnabled = true
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

                                    // Otorgar permisos de Micrófono y Cámara a HTML5 / WebRTC
                                    override fun onPermissionRequest(request: PermissionRequest?) {
                                        Log.d("AfroditaWebView", "Permiso Web solicitado: ${request?.resources?.joinToString()}")
                                        runOnUiThread {
                                            request?.grant(request.resources)
                                        }
                                    }

                                    // Habilitar Selector de Archivos (Fotos, Galería, Cámara) en HTML5 <input type="file">
                                    override fun onShowFileChooser(
                                        webView: WebView?,
                                        filePathCallbackIn: ValueCallback<Array<Uri>>?,
                                        fileChooserParams: FileChooserParams?
                                    ): Boolean {
                                        filePathCallback?.onReceiveValue(null)
                                        filePathCallback = filePathCallbackIn

                                        val contentIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                            addCategory(Intent.CATEGORY_OPENABLE)
                                            type = "image/*"
                                        }

                                        val chooserIntent = Intent.createChooser(contentIntent, "Selecciona una Foto / Selfie")
                                        try {
                                            fileChooserLauncher.launch(chooserIntent)
                                        } catch (e: Exception) {
                                            filePathCallback = null
                                            return false
                                        }
                                        return true
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

                                if (savedInstanceState != null) {
                                    restoreState(savedInstanceState)
                                } else {
                                    loadUrl("file:///android_asset/index.html")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    private fun requestNativePermissions() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val missing = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1001)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView?.saveState(outState)
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
