package com.example.dansr

import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
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
                VideoCapture()
            }
        }
    }
}



@Composable
fun VideoCapture() {
    val context = LocalContext.current
    var videoUri by remember { mutableStateOf<Uri?>(null) }

    // Création d'un fichier temporaire pour stocker la vidéo
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
            videoUri = videoFileUri
        } else {
            Toast.makeText(context, "Échec de l'enregistrement vidéo", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            //Ajout try catch
            try {
                takeVideoLauncher.launch(videoFileUri)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Erreur lors de l'ouverture de la caméra", Toast.LENGTH_LONG).show()
            }
        }) {
            Text("Record Video")
        }
        Spacer(modifier = Modifier.height(16.dp))

        videoUri?.let { uri ->
            AndroidView(
                //ctx = context
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(uri)
                        start()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
        }
    }
}

