package com.example.dansr

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import markVideoAsSaved
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

@Composable
fun VideoPlayerScreen(context: Context) {
    var videoList by remember { mutableStateOf(listOf<Uri>()) }
    var currentVideoIndex by remember { mutableIntStateOf(0) }
    val player = remember { ExoPlayer.Builder(context).build() }
    var dragState by remember { mutableStateOf(DragState.IDLE) }

    // Function to add a new random video if needed
    fun addRandomVideo() {
        getRandomVideoFromAssets(context)?.let { newVideoPath ->
            val newUri = Uri.parse("file:///android_asset/$newVideoPath")
            videoList = videoList + newUri // Add to list
        }
    }

    // Initialize with the first video
    LaunchedEffect(Unit) {
        addRandomVideo()
    }

    // Only update the player when necessary
    LaunchedEffect(currentVideoIndex) {
        if (videoList.isNotEmpty()) {
            val mediaItem = MediaItem.fromUri(videoList[currentVideoIndex])
            if (player.currentMediaItem?.localConfiguration?.uri != mediaItem.localConfiguration?.uri) {
                player.setMediaItem(mediaItem)
                player.prepare()
                player.playWhenReady = true
                player.repeatMode = ExoPlayer.REPEAT_MODE_ONE
            }
        }
    }

    // Ensure smooth lifecycle handling
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> player.pause()
                Lifecycle.Event.ON_RESUME -> player.play()
                Lifecycle.Event.ON_DESTROY -> {
                    player.playWhenReady = false
                    player.stop()

                    // Wait before releasing to avoid SurfaceTexture issues
                    CoroutineScope(Dispatchers.Main).launch {
                        player.setVideoSurface(null)  // Detach Surface
                        player.release()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            // Ensure the surface is detached before releasing
            player.playWhenReady = false
            player.stop()

            CoroutineScope(Dispatchers.Main).launch {
                player.setVideoSurface(null)  // Safely detach PlayerView
                player.release()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragState = DragState.DRAGGING },
                    onDrag = { _, dragAmount ->
                        if (videoList.isEmpty() || dragState != DragState.DRAGGING) return@detectDragGestures

                        val verticalThreshold = 100f
                        val horizontalThreshold = 100f

                        when {
                            // Scroll Up → Next Video
                            dragAmount.y < -verticalThreshold -> {
                                if (currentVideoIndex == videoList.size - 1) addRandomVideo()
                                currentVideoIndex = (currentVideoIndex + 1).coerceAtMost(videoList.size - 1)
                                dragState = DragState.IDLE
                            }
                            // Scroll Down → Previous Video
                            dragAmount.y > verticalThreshold -> {
                                currentVideoIndex = (currentVideoIndex - 1).coerceAtLeast(0)
                                dragState = DragState.IDLE
                            }
                            // Scroll Left -> Save Video
                            dragAmount.x < -horizontalThreshold -> {
                                val currentVideoUri = videoList[currentVideoIndex]
                                val fileName = currentVideoUri.toString().substringAfterLast("/")
                                markVideoAsSaved(context, fileName)
                                dragState = DragState.IDLE
                            }
                            // Scroll Right → Placeholder
                            dragAmount.x > horizontalThreshold -> {
                                dragState = DragState.IDLE
                            }
                        }
                    },
                    onDragEnd = {
                        dragState = DragState.IDLE
                    }
                )
            }
    ) {
        if (videoList.isNotEmpty()) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}


fun getRandomVideoFromAssets(context: Context): String? {
    val folderName = "videos" // Updated to use the unified folder

    return try {
        // Get all MP4 files from the videos folder
        val files = context.assets.list(folderName)
            ?.filter { it.endsWith(".mp4", ignoreCase = true) }
            ?.map { "$folderName/$it" }

        // Return random video or null if no videos exist
        files?.randomOrNull()
    } catch (e: Exception) {
        Log.e("VideoLoader", "Error loading videos from assets", e)
        null
    }
}

enum class DragState {
    IDLE, DRAGGING
}
