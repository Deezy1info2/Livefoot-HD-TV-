package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.BuildConfig

class MainActivity : ComponentActivity() {
  private var backPressedTime = 0L
  private var webView: WebView? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Keep screen on when app is running
    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    // Double back press to exit or go back in WebView
    onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        val wv = webView
        if (wv != null && wv.canGoBack()) {
          wv.goBack()
        } else {
          if (backPressedTime + 2000 > System.currentTimeMillis()) {
            finish()
          } else {
            android.widget.Toast.makeText(this@MainActivity, "Press back again to exit", android.widget.Toast.LENGTH_SHORT).show()
            backPressedTime = System.currentTimeMillis()
          }
        }
      }
    })

    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          WebViewScreen(
            modifier = Modifier.padding(innerPadding),
            onWebViewCreated = { wv -> webView = wv }
          )
        }
      }
    }
  }
}

@Composable
fun LiveScoreOverlay(
    modifier: Modifier = Modifier,
    viewModel: LiveScoreViewModel = viewModel()
) {
    val liveScores by viewModel.liveScores.collectAsState()
    
    // Trigger fetch
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.fetchLiveScores(BuildConfig.LIVESCORE_API_KEY)
    }
    
    Card(modifier = modifier) {
        LazyColumn {
            items(liveScores) { match ->
                Text(text = "${match.teams.home.name} vs ${match.teams.away.name}")
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
  modifier: Modifier = Modifier,
  onWebViewCreated: (WebView) -> Unit = {}
) {
  AndroidView(
    modifier = modifier.fillMaxSize(),
    factory = { context ->
      val swipeRefreshLayout = SwipeRefreshLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT
        )
        setColorSchemeResources(
          android.R.color.holo_blue_bright,
          android.R.color.holo_green_light,
          android.R.color.holo_orange_light,
          android.R.color.holo_red_light
        )
      }

      val webView = WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT
        )
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.javaScriptCanOpenWindowsAutomatically = true
        addJavascriptInterface(WebAppInterface(context), "Android")
        
        webViewClient = object : WebViewClient() {
          override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            swipeRefreshLayout.isRefreshing = false
          }
        }
        
        webChromeClient = object : WebChromeClient() {
          override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
            Log.d("WebViewJS", "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
            return super.onConsoleMessage(consoleMessage)
          }
        }
        loadUrl("https://livefoot-hd.pages.dev/")
      }

      onWebViewCreated(webView)

      swipeRefreshLayout.addView(webView)
      swipeRefreshLayout.setOnRefreshListener {
        webView.reload()
      }

      swipeRefreshLayout
    }
  )
}

class WebAppInterface(private val context: Context) {
  @android.webkit.JavascriptInterface
  fun playStream(url: String) {
    Log.d("WebAppInterface", "playStream called with URL: $url")
    val intent = Intent(context, PlayerActivity::class.java).apply {
      putExtra("STREAM_URL", url)
    }
    context.startActivity(intent)
  }
}
