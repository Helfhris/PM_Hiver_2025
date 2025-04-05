import android.net.Uri
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun LearningScreen(videoPath: String, navController: NavController) {
    var isRecording by remember { mutableStateOf(false) }
    var userVideoUri by remember { mutableStateOf<Uri?>(null) }

    when {
        userVideoUri != null -> {
            // Afficher les deux vidéos côte à côte
            CompareDanceVideos(videoPath, userVideoUri!!)
        }
        isRecording -> {
            // Lancer l'enregistrement
            LaunchCameraForRecording { uri ->
                userVideoUri = uri
                isRecording = false
            }
        }
        else -> {
            // Affichage normal avec les boutons
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
fun LaunchCameraForRecording(
    onVideoRecorded: (Uri) -> Unit
) {
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
            Toast.makeText(context, "L'enregistrement a échoué", Toast.LENGTH_SHORT).show()
        }
    }

    // Appel direct au launcher
    LaunchedEffect(Unit) {
        captureLauncher.launch(videoUri)
    }
}


@Composable
fun CompareDanceVideos(
    modelVideoPath: String,
    userVideoUri: Uri
) {
    val context = LocalContext.current
    val modelPlayer = remember { createExoPlayerWithAssets(context, modelVideoPath) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Vidéo du modèle
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = modelPlayer
                    useController = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // moitié de l’écran
                .padding(8.dp)
        )

        // Vidéo de l'utilisateur
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(userVideoUri)
                    setOnPreparedListener { it.isLooping = true }
                    start()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // moitié de l’écran
                .padding(8.dp)
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            modelPlayer.release()
        }
    }
}

