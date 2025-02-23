package com.example.dansr

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.example.dansr.ui.theme.DansRTheme
import java.io.File



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DansRTheme {
                VideoCaptureScreen()
            }
        }
    }
}

@Composable
fun VideoCaptureScreen() {
    val context = LocalContext.current
    var videoUri by remember { mutableStateOf<Uri?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Vérifie si une vidéo est sélectionnée ou capturée
        if (videoUri != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                VideoPlayer(videoUri = videoUri!!)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = { videoUri = null }) {
                            Text("Recommencer")
                        }
                        Button(onClick = { }) {
                            Text("Publier")
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Partie haute avec couleur de fond rose
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFFF1C6D3)) // Rose pale
                        .padding(16.dp)
                ) {
                    // Titre
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 50.dp)
                            .wrapContentHeight()
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            "Sélectionnez !!",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    // Image du doigt qui clique
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .wrapContentHeight()
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.touching),
                            contentDescription = "Touching Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }

                    // Ouvrir la galerie pour sélectionner une vidéo
                    val pickVideoLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                        if (uri != null) {
                            videoUri = uri
                        } else {
                            Toast.makeText(context, "Aucune vidéo sélectionnée", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // Bouton pour choisir la vidéo
                    Button(
                        onClick = {
                            pickVideoLauncher.launch("video/*")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(top = 16.dp)
                    ) {
                        Text("Voir votre galerie")
                    }
                }

                // Partie basse avec couleur de fond verte
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFFB2E8B1))// Vert pale
                        .padding(16.dp)
                ) {
                    // Titre
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 50.dp)
                            .wrapContentHeight()
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            "Dansez !!",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    // Image du stickman qui danse
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .wrapContentHeight()
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.dancing),
                            contentDescription = "Dancing Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }

                    // Bouton pour prendre une vidéo
                    CaptureVideoButton(
                        onVideoCaptured = { uri ->
                            videoUri = uri
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(top = 16.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun CaptureVideoButton(onVideoCaptured: (Uri) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val videoFile = remember {
        val cacheDir = context.externalCacheDir ?: context.cacheDir
        File(cacheDir, "video_${System.currentTimeMillis()}.mp4")
    }
    val videoFileUri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.provider", videoFile)
    }

    val takeVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success: Boolean ->
        if (success) {
            onVideoCaptured(videoFileUri)
        } else {
            Toast.makeText(context, "Échec de l'enregistrement vidéo", Toast.LENGTH_SHORT).show()
        }
    }

    Button(
        onClick = {
            try {
                takeVideoLauncher.launch(videoFileUri)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Erreur lors de l'ouverture de la caméra", Toast.LENGTH_LONG).show()
            }
        },
        modifier = modifier
    ) {
        Text("Prendre une vidéo")
    }
}



@Composable
fun VideoPlayer(videoUri: Uri) {
    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                setVideoURI(videoUri)
                setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.isLooping = true
                }
                start()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
