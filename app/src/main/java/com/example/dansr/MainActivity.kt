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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

/** 🎥 Écran principal : Capture et affichage de la vidéo */
@Composable
fun VideoCaptureScreen() {
    val context = LocalContext.current
    var videoUri by remember { mutableStateOf<Uri?>(null) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CaptureVideo { uri ->
            videoUri = uri
        }
        Spacer(modifier = Modifier.height(16.dp))
        videoUri?.let { uri -> VideoPlayer(uri) }
    }
}

/** 🎬 Fonction qui gère la capture vidéo */
@Composable
fun CaptureVideo(onVideoCaptured: (Uri) -> Unit) {
    val context = LocalContext.current

    // Création du fichier temporaire pour stocker la vidéo
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

    Button(onClick = {
        try {
            takeVideoLauncher.launch(videoFileUri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erreur lors de l'ouverture de la caméra", Toast.LENGTH_LONG).show()
        }
    }) {
        Text("Record Video")
    }
}

/** 🎥 Fonction qui affiche la vidéo enregistrée */
@Composable
fun VideoPlayer(videoUri: Uri) {
    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                setVideoURI(videoUri)
                start()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    )
}
