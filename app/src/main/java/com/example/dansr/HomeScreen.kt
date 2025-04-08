package com.example.dansr

import VideoStatus
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import loadVideoStatuses
import markVideoAsSaved
import saveVideoStatuses
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.math.abs

@Composable
fun VideoPlayerScreen(context: Context, navController: NavController) {
    var videoList by remember { mutableStateOf(listOf<Uri>()) }
    var currentVideoIndex by remember { mutableIntStateOf(0) }
    val player = remember { ExoPlayer.Builder(context).build() }
    var dragState by remember { mutableStateOf(DragState.IDLE) }
    val currentContext = LocalContext.current

    // Get current video status
    val currentVideoUri = if (videoList.isNotEmpty()) videoList[currentVideoIndex] else null
    val currentFileName = currentVideoUri?.toString()?.substringAfterLast("/")
    val videoStatuses = loadVideoStatuses(context)
    val currentVideoStatus = currentFileName?.let { fileName ->
        videoStatuses.find { it.fileName == fileName }
    }
    var isLiked by remember { mutableStateOf(currentVideoStatus?.isLiked ?: false) }

    // Update isLiked when video changes
    LaunchedEffect(currentVideoIndex) {
        isLiked = currentVideoStatus?.isLiked ?: false
    }

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
                    onDrag = { change, dragAmount ->
                        if (videoList.isEmpty()) return@detectDragGestures

                        // Use lower thresholds for better sensitivity
                        val verticalThreshold = 60f
                        val horizontalThreshold = 60f

                        // Consume the position change to prevent multiple triggers
                        change.consume()

                        when {
                            // Next Video (swipe up)
                            dragAmount.y < -verticalThreshold && abs(dragAmount.y) > abs(dragAmount.x) -> {
                                if (currentVideoIndex == videoList.size - 1) addRandomVideo()
                                currentVideoIndex = (currentVideoIndex + 1).coerceAtMost(videoList.size - 1)
                                dragState = DragState.COMPLETED
                            }
                            // Previous Video (swipe down)
                            dragAmount.y > verticalThreshold && abs(dragAmount.y) > abs(dragAmount.x) -> {
                                currentVideoIndex = (currentVideoIndex - 1).coerceAtLeast(0)
                                dragState = DragState.COMPLETED
                            }
                            // Save Video (swipe left)
                            dragAmount.x < -horizontalThreshold && abs(dragAmount.x) > abs(dragAmount.y) -> {
                                currentVideoUri?.let {
                                    val fileName = it.toString().substringAfterLast("/")
                                    markVideoAsSaved(context, fileName)
                                    // Add visual feedback
                                    Toast.makeText(context, "Video saved", Toast.LENGTH_SHORT).show()
                                }
                                dragState = DragState.COMPLETED
                            }
                            // Upload (swipe right)
                            dragAmount.x > horizontalThreshold && abs(dragAmount.x) > abs(dragAmount.y) -> {
                                currentVideoUri?.let { uri ->
                                    val fileName = uri.toString().substringAfterLast("/")
                                    val assetPath = "videos/$fileName"
                                    navController.currentBackStackEntry?.savedStateHandle?.set("videoPath", assetPath)
                                    navController.navigate("LearningScreen")
                                }
                                dragState = DragState.COMPLETED
                            }
                        }
                    },
                    onDragEnd = {
                        if (dragState == DragState.DRAGGING) {
                            dragState = DragState.IDLE
                        }
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

        // Overlay with buttons
        Box(modifier = Modifier.fillMaxSize()) {
            // Left button (HourglassBottom) - same as dragging left
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(16.dp)
                    .clickable {
                        currentVideoUri?.let {
                            val fileName = it.toString().substringAfterLast("/")
                            markVideoAsSaved(context, fileName)
                        }
                    }
            ) {
                Icon(
                    imageVector = Icons.Outlined.HourglassBottom,
                    contentDescription = "Save Video",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
            }

            // Right button (Add) - navigate to upload screen
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(16.dp)
                    .clickable {
                        navController.navigate(DansRScreen.Upload.name) {
                            popUpTo = navController.graph.findStartDestination().id
                            launchSingleTop = true
                        }
                    }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Upload",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
            }

            // Bottom center button (Favorite) - toggle like status
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .clickable {
                        currentFileName?.let { fileName ->
                            val statuses = loadVideoStatuses(context).toMutableList()
                            val existingStatus = statuses.find { it.fileName == fileName }

                            if (existingStatus != null) {
                                existingStatus.isLiked = !existingStatus.isLiked
                                isLiked = existingStatus.isLiked
                                val message = if (isLiked) "Added to likes!" else "Removed from likes"
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            } else {
                                statuses.add(VideoStatus(fileName, isLiked = true))
                                isLiked = true
                                Toast.makeText(context, "Added to likes!", Toast.LENGTH_SHORT).show()
                            }
                            saveVideoStatuses(context, statuses)
                        }
                    }
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) Color.Red.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
            }
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
    IDLE, DRAGGING, COMPLETED
}