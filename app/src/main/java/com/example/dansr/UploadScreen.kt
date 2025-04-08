package com.example.dansr

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import android.widget.VideoView
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import java.io.File
import publishUserVideo

@Composable
fun VideoCaptureScreen(navController: NavController) {
    val context = LocalContext.current
    var videoUri by remember { mutableStateOf<Uri?>(null) }

    // Video Picker Launcher
    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            videoUri = uri
        } else {
            Toast.makeText(context, "No video selected", Toast.LENGTH_SHORT).show()
        }
    }

    // Video Capture Launcher
    val videoFile = remember {
        val cacheDir = context.externalCacheDir ?: context.cacheDir
        File(cacheDir, "video_${System.currentTimeMillis()}.mp4")
    }
    val videoFileUri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.provider", videoFile)
    }

    val captureVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success: Boolean ->
        if (success) {
            videoUri = videoFileUri
        } else {
            Toast.makeText(context, "Failed to record video", Toast.LENGTH_SHORT).show()
        }
    }

    if (videoUri != null) {
        // Video Preview Screen
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
                    Button(onClick = { videoUri = null }) { Text(text = stringResource(id = R.string.upload_retry)) }
                    Button(onClick = {
                        videoUri?.let { uri ->
                            publishUserVideo(context, uri)

                            navController.navigate(DansRScreen.Start.name)
                        }
                    }) {
                        Text(text = stringResource(id = R.string.upload_publish))
                    }

                }
            }
        }
    } else {
        // Video Selection / Capture Screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // "From your gallery"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                    .padding(16.dp)
                    .clickable { pickVideoLauncher.launch("video/*") },
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(id = R.string.upload_gallery))

            }

            Spacer(modifier = Modifier.height(16.dp))

            // "From your camera"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                    .padding(16.dp)
                    .clickable { captureVideoLauncher.launch(videoFileUri) }, // Launch capture directly
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(id = R.string.upload_camera))
            }
        }
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