package io.hanko.hanko_elements

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import io.hanko.hanko_elements.ui.theme.HankoElementsTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val credentialManagerHandler = CredentialManagerHandler(this)
        WebView.setWebContentsDebuggingEnabled(true)
        enableEdgeToEdge()
        setContent {
            HankoElementsTheme {
                AndroidView(factory = {
                    WebView(it).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true

                        val url = "https://apps-metadata-provider.stg.hanko.io/login"
                        val listenerSupported = WebViewFeature.isFeatureSupported(
                            WebViewFeature.WEB_MESSAGE_LISTENER
                        )
                        if (listenerSupported) {
                            hookWebAuthnListener(
                                this,
                                this@MainActivity,
                                lifecycleScope,
                                credentialManagerHandler
                            )
                        } else {

                        }

                        loadUrl(url)
                    }
                })
            }
        }
    }

    @SuppressLint("RequiresFeature")
    private fun hookWebAuthnListener(
        webView: WebView,
        context: ComponentActivity,
        coroutineScope: CoroutineScope,
        credentialManagerHandler: CredentialManagerHandler
    ) {
        val passkeyWebListener =
            PasskeyWebListener(context, coroutineScope, credentialManagerHandler)

        val rules = setOf("*")
        WebViewCompat.addWebMessageListener(
            webView,
            PasskeyWebListener.INTERFACE_NAME,
            rules,
            passkeyWebListener
        )

        val webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                passkeyWebListener.onPageStarted()
                webView.evaluateJavascript(PasskeyWebListener.INJECTED_VAL, null)
            }
        }

        webView.webViewClient = webViewClient
    }
}