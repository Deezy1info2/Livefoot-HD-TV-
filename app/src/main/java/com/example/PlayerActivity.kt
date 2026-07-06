package com.example

import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.ui.PlayerView

class PlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Full screen
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val streamUrl = intent.getStringExtra("STREAM_URL")
        android.util.Log.d("PlayerActivity", "onCreate: STREAM_URL = $streamUrl")
        if (streamUrl.isNullOrEmpty()) {
            android.util.Log.e("PlayerActivity", "streamUrl is null or empty")
            finish()
        } else {
            setContent {
                PlayerScreen(streamUrl)
            }
        }
    }
}

@Composable
fun PlayerScreen(streamUrl: String) {
    val context = LocalContext.current
    var currentUrl by remember { mutableStateOf(streamUrl) }
    var areServersVisible by remember { mutableStateOf(true) }
    var interactionTrigger by remember { mutableStateOf(0) }
    
    // Auto-hide servers after 5 seconds of no interaction / idle
    LaunchedEffect(areServersVisible, interactionTrigger) {
        if (areServersVisible) {
            delay(5000L)
            areServersVisible = false
        }
    }
    
    val exoPlayer = remember {
        val loadControl = DefaultLoadControl.Builder()
            .build()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build().apply {
                setAudioAttributes(audioAttributes, true)
                playWhenReady = true
            }
    }

    LaunchedEffect(currentUrl) {
        val mediaItem = MediaItem.Builder()
            .setUri(currentUrl)
            .setMimeType(if (currentUrl.contains(".m3u8")) MimeTypes.APPLICATION_M3U8 else null)
            .build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        areServersVisible = true
                        interactionTrigger++
                    }
                }
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                        areServersVisible = (visibility == View.VISIBLE)
                        if (visibility == View.VISIBLE) {
                            interactionTrigger++
                        }
                    })
                }
            }
        )
        
        // Buttons container with smooth animated transitions
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = areServersVisible,
                enter = fadeIn(animationSpec = spring()) + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut(animationSpec = spring()) + slideOutVertically(targetOffsetY = { it })
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .padding(bottom = 24.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Server 1 Button (Plays main streamUrl)
                        Button(
                            onClick = { currentUrl = streamUrl },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentUrl == streamUrl) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                                contentColor = if (currentUrl == streamUrl) MaterialTheme.colorScheme.onPrimary else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp).padding(end = 4.dp)
                            )
                            Text("Server 1")
                        }

                        // Server 2 Button (Plays backup stream URL)
                        val backupUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
                        Button(
                            onClick = { currentUrl = backupUrl },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentUrl == backupUrl) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                                contentColor = if (currentUrl == backupUrl) MaterialTheme.colorScheme.onPrimary else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp).padding(end = 4.dp)
                            )
                            Text("Server 2")
                        }
                    }
                }
            }
        }
    }
}
