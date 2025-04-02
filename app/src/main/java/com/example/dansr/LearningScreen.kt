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

@Composable
fun LearningScreen(videoPath: String, navController: NavController) {
    val context = LocalContext.current
    val exoPlayer = remember { createExoPlayerWithAssets(context, videoPath) }
    var isPlaying by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Vidéo affichée en haut
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // Désactiver les contrôles natifs
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Occupe l'espace disponible
        )

        // Boutons de contrôle de la vidéo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Bouton Pause / Reprendre
            Button(
                onClick = {
                    isPlaying = !isPlaying
                    exoPlayer.playWhenReady = isPlaying
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPlaying) Color.Red else Color.Green
                )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Reprendre",
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Bouton Restart
            Button(
                onClick = {
                    exoPlayer.seekTo(0) // Recommencer depuis le début
                    exoPlayer.playWhenReady = true
                    isPlaying = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA500))
            ) {
                Icon(
                    imageVector = Icons.Filled.Replay,
                    contentDescription = "Recommencer",
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Bouton Drapeau (Start)
            Button(
                onClick = {
                    exoPlayer.pause() // Mettre la vidéo en pause
                    navController.navigate("CameraScreen") // Aller à l'écran de la caméra
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Icon(
                    imageVector = Icons.Filled.Flag,
                    contentDescription = "Drapeau",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }
}
