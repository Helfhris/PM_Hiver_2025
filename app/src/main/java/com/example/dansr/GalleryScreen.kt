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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dansr.DansRScreen
import com.example.dansr.R

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
        GalleryScreenContent(pages[page])
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
fun GalleryScreenContent(currentScreen: DansRScreen) {
    val rows = 10
    val columns = 3
    val placeholderList = List(rows) { List(columns) { R.drawable.videoplaceholder_dark } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(placeholderList) { row ->
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                items(row) { imageRes ->
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = "Video Placeholder",
                        modifier = Modifier
                            .size(120.dp)
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}
