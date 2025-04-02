import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.dansr.DansRScreen
import com.example.dansr.R
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@Composable
fun GalleryPagerScreen(currentScreen: DansRScreen, navController: NavController) {
    val pages = listOf(DansRScreen.Likes, DansRScreen.Saved, DansRScreen.Uploaded)
    val startIndex = pages.indexOf(currentScreen).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        GalleryScreenContent(pages[page], navController)
    }

    // Listen for page changes and update the navController
    LaunchedEffect(pagerState.currentPage) {
        if (pages[pagerState.currentPage] != currentScreen) {
            navController.navigate(pages[pagerState.currentPage].name) {
                popUpTo(navController.graph.startDestinationId)
                launchSingleTop = true
            }
        }
    }
}

@Composable
fun GalleryScreenContent(screen: DansRScreen, navController: NavController) {
    val context = LocalContext.current
    val videoFiles = remember { getVideoFilesFromAssets(context, screen) }

    var selectedVideo by remember { mutableStateOf<String?>(null) } // Vidéo en cours de lecture
    var learningVideo by remember { mutableStateOf<String?>(null) } // Vidéo sélectionnée pour l'apprentissage

    val columns = 3
    val rows = videoFiles.chunked(columns.coerceAtLeast(1)) // Groupement en lignes

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(rows) { row ->
                LazyRow(horizontalArrangement = Arrangement.Start) {
                    items(row) { videoFile ->
                        VideoThumbnail(videoPath = videoFile) {
                            selectedVideo = videoFile // Ouvrir la vidéo au clic
                        }
                    }
                }
            }
        }

        // Overlay lorsque la vidéo est en cours de lecture
        selectedVideo?.let { videoPath ->
            val exoPlayer = remember { createExoPlayerWithAssets(context, videoPath) }
            var isPlaying by remember { mutableStateOf(true) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.8f))
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                        .align(Alignment.Center)
                        .clickable {
                            isPlaying = !isPlaying
                            exoPlayer.playWhenReady = isPlaying
                        }
                )

                // Bouton "Apprendre cette danse"
                Button(
                    onClick = {
                        learningVideo = videoPath // Marquer la vidéo pour l'apprentissage
                        selectedVideo = null      // Fermer la lecture
                        exoPlayer.release()
                        navController.currentBackStackEntry?.savedStateHandle?.set("videoPath", videoPath)
                        navController.navigate("LearningScreen")

                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text("Apprendre cette danse")
                }

                // Bouton de fermeture
                androidx.compose.material3.Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Fermer la vidéo",
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier
                        .padding(16.dp)
                        .size(32.dp)
                        .align(Alignment.TopStart)
                        .clickable {
                            exoPlayer.release()
                            selectedVideo = null
                        }
                )
            }
        }
    }
}





@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerFromAssets(videoPath: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exoPlayer = remember { createExoPlayerWithAssets(context, videoPath) }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM // Crops to fit
            }
        },
        modifier = Modifier
    )

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }
}


@Composable
fun VideoThumbnail(videoPath: String, onClick: () -> Unit) {
    val context = LocalContext.current
    val thumbnailBitmap = remember { extractVideoThumbnail(context, videoPath) }

    thumbnailBitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "Video Thumbnail",
            modifier = Modifier
                .size(120.dp)
                .padding(4.dp)
                .clickable { onClick() } // Click to play video
        )
    }
}

fun extractVideoThumbnail(context: Context, filePath: String): Bitmap? {
    return try {
        val retriever = MediaMetadataRetriever()
        val assetFileDescriptor = context.assets.openFd(filePath)
        retriever.setDataSource(assetFileDescriptor.fileDescriptor, assetFileDescriptor.startOffset, assetFileDescriptor.length)
        val bitmap = retriever.getFrameAtTime(0) // First frame
        retriever.release()
        bitmap
    } catch (e: Exception) {
        Log.e("ThumbnailError", "Failed to load thumbnail: ${e.message}")
        null
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

fun getVideoFilesFromAssets(context: Context, screen: DansRScreen): List<String> {
    val folderName = when (screen) {
        DansRScreen.Likes -> "Likes"
        DansRScreen.Saved -> "Saved"
        DansRScreen.Uploaded -> "Uploaded"
        else -> ""
    }

    return try {
        context.assets.list(folderName)?.filter { it.endsWith(".mp4") }?.map { "$folderName/$it" } ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}