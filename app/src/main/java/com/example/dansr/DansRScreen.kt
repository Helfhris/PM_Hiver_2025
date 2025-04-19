package com.example.dansr

import LearningScreen
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.PlayLesson
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument


enum class DansRScreen(@StringRes val title: Int) {
    Start(title = R.string.app_name),
    Likes(title = R.string.likes),
    Saved(title = R.string.saved),
    Uploaded(title = R.string.uploaded),
    Upload(title = R.string.upload),
    DanceRSS(title = R.string.resources),
    LearningScreen(title = R.string.learningScreen)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DansRAppBar(
    currentScreen: DansRScreen,
    modifier: Modifier = Modifier
) {
    val screenIcons = mapOf(
        DansRScreen.Start to Icons.Outlined.Home,
        DansRScreen.Likes to Icons.Outlined.ViewWeek,
        DansRScreen.Saved to Icons.Outlined.ViewWeek,
        DansRScreen.Uploaded to Icons.Outlined.ViewWeek,
        DansRScreen.Upload to Icons.Outlined.FileUpload,
        DansRScreen.DanceRSS to Icons.Outlined.School,
        DansRScreen.LearningScreen to Icons.Outlined.PlayLesson
    )
    val screenIconDescriptions = mapOf(
        DansRScreen.Start to R.string.start_icon,
        DansRScreen.Likes to R.string.gallery_icon,
        DansRScreen.Saved to R.string.gallery_icon,
        DansRScreen.Uploaded to R.string.gallery_icon,
        DansRScreen.Upload to R.string.upload_icon,
        DansRScreen.DanceRSS to R.string.resources_icon,
        DansRScreen.LearningScreen to R.string.resources_icon
    )
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ){
                    Image(
                        painter = painterResource(id = R.drawable.logo_dansr_blanc),
                        contentDescription = stringResource(R.string.logo_description),
                        modifier = Modifier
                            .size(40.dp)
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ){
                    Icon(
                        imageVector = screenIcons[currentScreen] ?: Icons.Outlined.Home,
                        contentDescription = stringResource(screenIconDescriptions[currentScreen] ?: R.string.start_icon),
                        modifier = Modifier.size(40.dp),
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ){
                    Spacer(modifier = Modifier.size(width = 55.dp, height = 0.dp))
                    /*Icon(
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = stringResource(R.string.user_icon),
                        modifier = Modifier.size(40.dp)
                    )*/ // We didn't make user accounts in the current version
                }
            }
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = modifier
    )
}


@Composable
fun GalleryTopBar(currentScreen: DansRScreen, navController: NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(MaterialTheme.colorScheme.tertiaryContainer),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val screens = listOf(DansRScreen.Likes, DansRScreen.Saved, DansRScreen.Uploaded)
        val icons = listOf(
            Icons.Outlined.FavoriteBorder,
            Icons.Outlined.HourglassBottom,
            Icons.Outlined.StarBorder
        )
        val iconDescriptions = listOf(
            R.string.likes_icon,
            R.string.saved_icon,
            R.string.uploaded_icon
        )

        screens.forEachIndexed { index, screen ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = {
                        if (currentScreen != screen) {
                            navController.navigate(screen.name) {
                                popUpTo(navController.graph.findStartDestination().id)
                                launchSingleTop = true
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = icons[index],
                        contentDescription = stringResource(iconDescriptions[index]),
                        tint = if (currentScreen == screen) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}


@Composable
fun BottomBar(currentScreen: DansRScreen, navController: NavHostController) {
    val (screens, icons, iconDescriptions) =
        Triple(
            listOf(DansRScreen.Start, DansRScreen.Likes, DansRScreen.DanceRSS, DansRScreen.Upload),
            listOf(
                Icons.Outlined.Home,
                Icons.Outlined.ViewWeek,
                Icons.Outlined.School,
                Icons.Outlined.FileUpload
            ),
            listOf(
                R.string.start_icon,
                R.string.gallery_icon,
                R.string.resources_icon,
                R.string.upload_icon
            )
        )

    // Iterate over the screens and show the corresponding icons
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        screens.forEachIndexed { index, screen ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = {
                        if (currentScreen != screen) {
                            navController.navigate(screen.name) {
                                popUpTo(navController.graph.findStartDestination().id)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = icons[index],
                        contentDescription = stringResource(iconDescriptions[index]),
                        tint = if (
                            screen == DansRScreen.Likes &&
                            (currentScreen == DansRScreen.Likes ||
                                    currentScreen == DansRScreen.Uploaded ||
                                    currentScreen == DansRScreen.Saved)
                        ) {
                            MaterialTheme.colorScheme.primary
                        } else if (currentScreen == screen) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}





@Composable
fun DansRApp(
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()

    val currentRoute = backStackEntry?.destination?.route
    val currentScreen = when {
        currentRoute?.startsWith("LearningScreen") == true -> DansRScreen.LearningScreen
        DansRScreen.entries.any { it.name == currentRoute } -> DansRScreen.entries.first { it.name == currentRoute }
        else -> DansRScreen.Start
    }


    val showGalleryTopBar = currentScreen in listOf(DansRScreen.Likes, DansRScreen.Saved, DansRScreen.Uploaded)

    Scaffold(
        topBar = {
            Column {
                DansRAppBar(
                    currentScreen = currentScreen,
                )

                if (showGalleryTopBar) {
                    GalleryTopBar(currentScreen, navController)
                }
            }
        },
        bottomBar = {
            BottomBar(currentScreen = currentScreen, navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = DansRScreen.Start.name,
            modifier = Modifier.padding(innerPadding)
        ) { // Every route in the app
            composable(route = DansRScreen.Start.name) {
                VideoPlayerScreen(context = LocalContext.current, navController = navController)
            }
            composable(route = DansRScreen.Likes.name) {
                GalleryPagerScreen(currentScreen = currentScreen, navController = navController)
            }
            composable(route = DansRScreen.Saved.name) {
                GalleryPagerScreen(currentScreen = currentScreen, navController = navController)
            }
            composable(route = DansRScreen.Uploaded.name) {
                GalleryPagerScreen(currentScreen = currentScreen, navController = navController)
            }
            composable(route = DansRScreen.Upload.name) {
                VideoCaptureScreen(navController = navController)
            }
            composable(route = DansRScreen.DanceRSS.name) {
                DancingResourcesScreenContent()
            }
            composable(
                route = "LearningScreen/{videoPath}",
                arguments = listOf(navArgument("videoPath") { type = NavType.StringType })
            ) { backStackEntry ->
                val videoPath = backStackEntry.arguments?.getString("videoPath") ?: ""
                LearningScreen(videoPath, navController)
            }
        }
    }
}