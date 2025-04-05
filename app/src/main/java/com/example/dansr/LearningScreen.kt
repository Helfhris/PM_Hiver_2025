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
import androidx.compose.material3.MaterialTheme
import androidx.core.content.FileProvider
import java.io.File
import createExoPlayerWithAssets

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
fun CompareDanceVideos(modelVideoPath: String, userVideoUri: Uri) {
    val context = LocalContext.current
    val modelPlayer = remember { createExoPlayerWithAssets(context, modelVideoPath) }
    val videoViewRef = remember { mutableStateOf<VideoView?>(null) }

    var resyncTrigger by remember { mutableStateOf(false) }
    var isModelFinished by remember { mutableStateOf(false) }

    // Observateur de fin de la vidéo modèle
    LaunchedEffect(Unit) {
        modelPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    // La vidéo modèle est terminée, on déclenche la synchronisation
                    isModelFinished = true
                    resyncTrigger = true
                }
            }
        })
    }

    // Observateur de fin de la vidéo utilisateur
    LaunchedEffect(Unit) {
        videoViewRef.value?.setOnCompletionListener {
            if (!isModelFinished) {
                // Si la vidéo de l'utilisateur finit avant la vidéo modèle, on laisse la vidéo modèle se finir
                return@setOnCompletionListener
            }
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // Bouton "Recommencer" (gauche - 15%)
        Column(
            modifier = Modifier
                .weight(0.15f)
                .fillMaxHeight(), // Retirer le padding pour toute la hauteur
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    modelPlayer.seekTo(0)
                    videoViewRef.value?.seekTo(0)
                    modelPlayer.playWhenReady = true
                    videoViewRef.value?.start()
                },
                modifier = Modifier.fillMaxHeight(), // Prendre toute la hauteur de la colonne
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red) // Couleur de fond rouge
            ) {
                Text(text = "Recommencer", color = Color.White)
            }
        }

        // Vidéos au centre (70%)
        Column(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight()
        ) {
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
                    .weight(1f)
            )

            // Vidéo de l'utilisateur
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(userVideoUri)
                        setOnPreparedListener { mp ->
                            mp.setOnCompletionListener {
                                // Si la vidéo de l'utilisateur finit avant la vidéo modèle, on attend la fin du modèle
                                if (!isModelFinished) {
                                    return@setOnCompletionListener
                                }
                                resyncTrigger = true
                            }
                            mp.isLooping = false
                            start()
                        }
                        videoViewRef.value = this
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }

        // Bouton "Publier" (droite - 15%)
        Column(
            modifier = Modifier
                .weight(0.15f)
                .fillMaxHeight(), // Retirer le padding pour toute la hauteur
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    // Navigation vers l'écran de publication
                },
                modifier = Modifier.fillMaxHeight(), // Prendre toute la hauteur de la colonne
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green) // Couleur de fond verte
            ) {
                Text(text = "Publier", color = Color.White)
            }
        }
    }

    // 🔄 Resync quand la vidéo modèle se termine
    LaunchedEffect(resyncTrigger) {
        if (resyncTrigger) {
            // Remettre les deux vidéos au début
            modelPlayer.seekTo(0)
            videoViewRef.value?.seekTo(0)

            // Petite pause pour synchroniser
            kotlinx.coroutines.delay(200)

            // Démarrer les deux vidéos en même temps
            modelPlayer.playWhenReady = true
            videoViewRef.value?.start()

            resyncTrigger = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            modelPlayer.release()
        }
    }
}

