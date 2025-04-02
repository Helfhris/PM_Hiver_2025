package com.example.dansr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import createExoPlayerWithAssets
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout

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
            Button(onClick = {
                isPlaying = !isPlaying
                exoPlayer.playWhenReady = isPlaying
            }) {
                Text(if (isPlaying) "Pause" else "Reprendre")
            }

            Button(onClick = {
                exoPlayer.seekTo(0) // Recommencer depuis le début
                exoPlayer.playWhenReady = true
                isPlaying = true
            }) {
                Text("Restart")
            }

            Button(onClick = {
                exoPlayer.pause() // Mettre la vidéo en pause
                navController.navigate("CameraScreen") // Aller à l'écran de la caméra
            }) {
                Text("Start")
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }
}
