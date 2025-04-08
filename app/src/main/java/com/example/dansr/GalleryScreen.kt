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
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
import android.net.Uri
import android.util.Log
import android.util.LruCache
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ThumbnailCache {
    private val cache = LruCache<String, Bitmap>(20) // Keep 20 thumbnails in memory

    fun getThumbnail(path: String): Bitmap? = cache.get(path)
    fun putThumbnail(path: String, bitmap: Bitmap) = cache.put(path, bitmap)
}

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

@OptIn(UnstableApi::class)
@Composable
fun GalleryScreenContent(screen: DansRScreen, navController: NavController) {
    val context = LocalContext.current
    val videoFiles by remember { mutableStateOf(getAllVideoFiles(context, screen)) }
    var selectedVideo by remember { mutableStateOf<String?>(null) }
    var videoDimensions by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var learningVideo by remember { mutableStateOf<String?>(null) } // Selected video to learn

    val columns = 3
    val rows = videoFiles.chunked(columns.coerceAtLeast(1)) // Video grouped in rows
    LaunchedEffect(selectedVideo) {
        selectedVideo?.let { path ->
            videoDimensions = withContext(Dispatchers.IO) {
                getVideoDimensions(context, path)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Grid of thumbnails
        LazyVerticalGrid(
            columns = GridCells.Adaptive(120.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(videoFiles) { videoPath ->
                AsyncVideoThumbnail(videoPath = videoPath) {
                    selectedVideo = videoPath // Set the selected video when clicked
                }
            }
        }

        // Fullscreen video player overlay
        selectedVideo?.let { videoPath ->
            val exoPlayer = remember {
                ExoPlayer.Builder(context)
                    .setMediaSourceFactory(DefaultMediaSourceFactory(context))
                    .build()
                    .apply {
                        val uri = if (videoPath.startsWith("videos/")) {
                            Uri.parse("asset:///$videoPath")
                        } else {
                            Uri.fromFile(File(videoPath))
                        }
                        setMediaItem(MediaItem.fromUri(uri))
                        prepare()
                        playWhenReady = true
                    }
            }

            DisposableEffect(exoPlayer) {
                onDispose { exoPlayer.release() }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                            setControllerShowTimeoutMs(0) // Always show Controller
                            setControllerAutoShow(true)
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (videoDimensions?.let { it.first < it.second } == true) {
                                Modifier.fillMaxHeight()
                            } else {
                                Modifier.aspectRatio(16f / 9f)
                            }
                        )
                        .align(Alignment.Center)
                )

                // Button "Learn this Dance"
                Button(
                    onClick = {
                        learningVideo = videoPath // Put Video as to be Learned
                        selectedVideo = null      // Close Playing
                        exoPlayer.release()
                        navController.currentBackStackEntry?.savedStateHandle?.set("videoPath", videoPath)
                        navController.navigate("LearningScreen")

                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text("Learn this Dance")
                }

                // Close button
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close video",
                    tint = Color.White,
                    modifier = Modifier
                        .padding(16.dp)
                        .size(32.dp)
                        .align(Alignment.TopStart)
                        .clickable { selectedVideo = null }
                )
            }
        }
    }
}

@Composable
fun AsyncVideoThumbnail(videoPath: String, onClick: () -> Unit) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(null) {
        value = ThumbnailCache.getThumbnail(videoPath) ?:
                withContext(Dispatchers.IO) {
                    extractVideoThumbnail(context, videoPath)?.also {
                        ThumbnailCache.putThumbnail(videoPath, it)
                    }
                }
    }

    Box(
        modifier = Modifier
            .size(120.dp)
            .padding(4.dp)
            .clickable(onClick = onClick)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Video thumbnail",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            PlaceholderThumbnail()
        }
    }
}

@Composable
fun PlaceholderThumbnail() {
    Box(
        modifier = Modifier
            .size(120.dp)
            .padding(4.dp)
            .background(Color.Gray.copy(alpha = 0.2f))
    )
}

fun extractVideoThumbnail(context: Context, filePath: String): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        if (filePath.startsWith("videos/")) {
            context.assets.openFd(filePath).use { fd ->
                retriever.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            }
        } else {
            retriever.setDataSource(filePath)
        }

        retriever.frameAtTime?.also { original ->
            val maxSize = 240
            val scale = maxSize.toFloat() / maxOf(original.width, original.height)
            Bitmap.createScaledBitmap(
                original,
                (original.width * scale).toInt(),
                (original.height * scale).toInt(),
                true
            )
        }
    } catch (e: Exception) {
        Log.e("Thumbnail", "Error loading $filePath", e)
        null
    } finally {
        retriever.release()
    }
}


fun getVideoFilesFromAssets(context: Context, screen: DansRScreen): List<String> {
    val statuses = loadVideoStatuses(context)
    val allVideos = context.assets.list("videos")?.filter { it.endsWith(".mp4") } ?: emptyList()

    return allVideos.filter { video ->
        val status = statuses.find { it.fileName == video }
        when (screen) {
            DansRScreen.Likes -> status?.isLiked == true
            DansRScreen.Saved -> status?.isSaved == true
            DansRScreen.Uploaded -> status?.isUploaded == true
            else -> false
        }
    }.map { "videos/$it" } // Return full path
}

fun getVideoFilesFromInternal(context: Context, screen: DansRScreen): List<String> {
    val statuses = loadVideoStatuses(context)
    val videoDir = context.filesDir.resolve("videos")
    if (!videoDir.exists() || !videoDir.isDirectory) return emptyList()

    return videoDir.listFiles { file -> file.extension == "mp4" }?.filter { file ->
        val status = statuses.find { it.fileName == file.name }
        when (screen) {
            DansRScreen.Likes -> status?.isLiked == true
            DansRScreen.Saved -> status?.isSaved == true
            DansRScreen.Uploaded -> status?.isUploaded == true
            else -> false
        }
    }?.map { it.absolutePath } ?: emptyList()
}

fun getAllVideoFiles(context: Context, screen: DansRScreen): List<String> {
    val assets = getVideoFilesFromAssets(context, screen)
    val internal = getVideoFilesFromInternal(context, screen)
    return assets + internal
}


fun getVideoDimensions(context: Context, videoPath: String): Pair<Int, Int> {
    val retriever = MediaMetadataRetriever()
    return try {
        if (videoPath.startsWith("videos/")) {
            context.assets.openFd(videoPath).use { fd ->
                retriever.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            }
        } else {
            retriever.setDataSource(videoPath)
        }
        val width = retriever.extractMetadata(METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
        val height = retriever.extractMetadata(METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
        width to height
    } finally {
        retriever.release()
    }
}
