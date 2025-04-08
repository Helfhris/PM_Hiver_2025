import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import createExoPlayerWithAssets
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.dansr.DansRScreen
import java.io.File

@Composable
fun LearningScreen(videoPath: String, navController: NavController) {
    var isRecording by remember { mutableStateOf(false) }
    var userVideoUri by remember { mutableStateOf<Uri?>(null) }

    when {
        userVideoUri != null -> {
            CompareDanceVideos(
                modelVideoPath = videoPath,
                userVideoUri = userVideoUri!!,
                navController = navController,
                onReplay = {
                    userVideoUri = null
                    isRecording = true
                }
            )
        }
        isRecording -> {
            LaunchCameraForRecording { uri ->
                userVideoUri = uri
                isRecording = false
            }
        }
        else -> {
            VideoWithControls(
                videoPath = videoPath,
                onStart = { isRecording = true }
            )
        }
    }
}

@Composable
fun VideoWithControls(videoPath: String, onStart: () -> Unit) {
    val context = LocalContext.current
    val exoPlayer = remember { createExoPlayerWithAssets(context, videoPath) }
    var isPlaying by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
            }},
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = {
                    isPlaying = !isPlaying
                    exoPlayer.playWhenReady = isPlaying
                },
                modifier = Modifier
                    .background(if (isPlaying) Color.Red else Color.Green)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(
                onClick = {
                    exoPlayer.seekTo(0)
                    exoPlayer.playWhenReady = true
                    isPlaying = true
                },
                modifier = Modifier.background(Color(0xFFFFA500)).padding(8.dp)
            ) {
                Icon(Icons.Default.Replay, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }

            IconButton(
                onClick = {
                    exoPlayer.pause()
                    onStart()
                },
                modifier = Modifier.background(Color(0xFF9C27B0)).padding(8.dp)
            ) {
                Icon(Icons.Default.Flag, contentDescription = "Start", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }
}


@Composable
fun LaunchCameraForRecording(onVideoRecorded: (Uri) -> Unit) {
    val context = LocalContext.current
    val videoFile = remember {
        val cacheDir = context.externalCacheDir ?: context.cacheDir
        File(cacheDir, "user_dance_${System.currentTimeMillis()}.mp4")
    }
    val videoUri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.provider", videoFile)
    }

    val captureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success) {
            onVideoRecorded(videoUri)
        } else {
            Toast.makeText(context, "Recording failed", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        captureLauncher.launch(videoUri)
    }
}


@Composable
fun CompareDanceVideos(modelVideoPath: String, userVideoUri: Uri, navController: NavController,onReplay: () -> Unit) {
    val context = LocalContext.current
    var isSwapped by remember { mutableStateOf(false) }
    var showIcons by remember { mutableStateOf(false) }

    val modelPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val assetUri = Uri.parse("asset:///$modelVideoPath")
            setMediaItem(MediaItem.fromUri(assetUri))
            prepare()
            playWhenReady = true
        }
    }

    val userPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(userVideoUri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    modelPlayer.seekTo(0)
                    modelPlayer.playWhenReady = true

                    userPlayer.seekTo(0)
                    userPlayer.playWhenReady = true
                }
            }
        }

        modelPlayer.addListener(listener)

        onDispose {
            modelPlayer.removeListener(listener)
            modelPlayer.release()
            userPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        isSwapped = !isSwapped
                        modelPlayer.seekTo(0)
                        modelPlayer.playWhenReady = true
                        userPlayer.seekTo(0)
                        userPlayer.playWhenReady = true
                    },
                    onTap = {
                        showIcons = !showIcons
                    }
                )
            }
    ) {
        // Display videos
        if (!isSwapped) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = modelPlayer
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopEnd
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = userPlayer
                            useController = false
                        }
                    },
                    modifier = Modifier
                        .size(width = 200.dp, height = 300.dp)
                        .padding(top = 20.dp)
                )
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = userPlayer
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopEnd
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = modelPlayer
                            useController = false
                        }
                    },
                    modifier = Modifier
                        .size(width = 200.dp, height = 300.dp)
                        .padding(top = 20.dp)
                )
            }
        }

        // Display conditional icons
        if (showIcons) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {onReplay()},
                    modifier = Modifier
                        .background(Color(0xFFFF9800), shape = MaterialTheme.shapes.medium)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Replay",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = {
                        publishUserVideo(context, userVideoUri)
                        showIcons = false
                        navController.navigate(DansRScreen.Start.name) {
                            popUpTo(0)
                        }
                    },
                    modifier = Modifier
                        .background(Color(0xFF4CAF50), shape = MaterialTheme.shapes.medium)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = "Publish",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}


fun publishUserVideo(context: Context, videoUri: Uri) {
    try {
        val videosDir = File(context.filesDir, "videos")
        if (!videosDir.exists()) videosDir.mkdirs()

        // Generate a unique name
        val existing = videosDir.listFiles()?.map { it.name } ?: emptyList()
        var index = 1
        var newFileName: String
        do {
            newFileName = "VideoUploaded$index.mp4"
            index++
        } while (newFileName in existing)

        val destFile = File(videosDir, newFileName)

        // Copy the video
        context.contentResolver.openInputStream(videoUri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        Log.d("PublishVideo", "Saved at: ${destFile.absolutePath}")

        // Update JSON
        val statuses = loadVideoStatuses(context).toMutableList()
        val existingStatus = statuses.find { it.fileName == newFileName }

        if (existingStatus != null) {
            existingStatus.isUploaded = true
        } else {
            statuses.add(VideoStatus(newFileName, isUploaded = true))
        }

        saveVideoStatuses(context, statuses)

        Toast.makeText(context, "Video published!", Toast.LENGTH_SHORT).show()

    } catch (e: Exception) {
        Log.e("PublishVideo", "Error during publication", e)
        Toast.makeText(context, "Publication failure", Toast.LENGTH_SHORT).show()
    }
}



fun createExoPlayerWithAssets(context: Context, filePath: String): ExoPlayer {
    val exoPlayer = ExoPlayer.Builder(context).build()

    val assetUri = Uri.parse("asset:///$filePath") // Works with subdirectories

    val mediaItem = MediaItem.fromUri(assetUri)
    exoPlayer.setMediaItem(mediaItem)
    exoPlayer.prepare()
    exoPlayer.playWhenReady = true

    return exoPlayer
}