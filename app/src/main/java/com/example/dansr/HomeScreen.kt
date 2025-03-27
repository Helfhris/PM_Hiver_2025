package com.example.dansr

import android.content.Context
import android.net.Uri
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
                                saveCurrentVideo(context, videoList[currentVideoIndex])
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

// Save video to internal storage
fun saveCurrentVideo(context: Context, uri: Uri) {
    val currentVideoPath = uri.path?.substringAfterLast("/")
    if (currentVideoPath != null) {
        val tempFile = File(context.cacheDir, currentVideoPath)
        copyVideoToInternalStorage(context, "Scrollable/$currentVideoPath", tempFile)
        copyVideoToSaved(context, tempFile, "Saved/$currentVideoPath")
        tempFile.delete()
    }
}


fun getRandomVideoFromAssets(context: Context): String? {
    val folderName = "Scrollable"

    return try {
        val files = context.assets.list(folderName)?.filter { it.endsWith(".mp4") }
        files?.randomOrNull()?.let { "$folderName/$it" }
    } catch (e: Exception) {
        null
    }
}

enum class DragState {
    IDLE, DRAGGING
}

fun copyVideoToInternalStorage(context: Context, sourcePath: String, destinationFile: File) {
    try {
        val inputStream: InputStream = context.assets.open(sourcePath)
        val outputStream = FileOutputStream(destinationFile)

        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

fun copyVideoToSaved(context: Context, sourceFile: File, destinationPath: String) {
    try {
        val outputFile = File(context.filesDir, destinationPath)
        outputFile.parentFile?.mkdirs()

        val inputStream = FileInputStream(sourceFile)
        val outputStream = FileOutputStream(outputFile)

        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
}