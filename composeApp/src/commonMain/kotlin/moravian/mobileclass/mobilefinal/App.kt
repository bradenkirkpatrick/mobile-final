package moravian.mobileclass.mobilefinal

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import mobilefinal.composeapp.generated.resources.Res
import mobilefinal.composeapp.generated.resources.nav_camera
import mobilefinal.composeapp.generated.resources.nav_camera_icon
import mobilefinal.composeapp.generated.resources.nav_home
import mobilefinal.composeapp.generated.resources.nav_home_icon
import mobilefinal.composeapp.generated.resources.nav_map
import mobilefinal.composeapp.generated.resources.nav_map_icon
import mobilefinal.composeapp.generated.resources.nav_photos
import mobilefinal.composeapp.generated.resources.nav_photos_icon
import org.jetbrains.compose.resources.stringResource

@Serializable
object HomeRoute

@Serializable
object PhotosRoute

@Serializable
object MapRoute

@Serializable
object CameraRoute

@Composable
@Preview
fun App() {
    MaterialTheme {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isLandscape = maxWidth > maxHeight
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail {
                        val homeSelected = currentDestination?.hasRoute<HomeRoute>() == true
                        val photosSelected = currentDestination?.hasRoute<PhotosRoute>() == true
                        val mapSelected = currentDestination?.hasRoute<MapRoute>() == true
                        val cameraSelected = currentDestination?.hasRoute<CameraRoute>() == true
                        NavigationRailItem(
                            selected = homeSelected,
                            enabled = !homeSelected,
                            onClick = {
                                navController.navigate(HomeRoute) {
                                    popUpTo(HomeRoute) { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                            icon = { Text(stringResource(Res.string.nav_home_icon)) },
                            label = { Text(stringResource(Res.string.nav_home)) },
                        )
                        NavigationRailItem(
                            selected = photosSelected,
                            enabled = !photosSelected,
                            onClick = {
                                navController.navigate(PhotosRoute) {
                                    popUpTo(HomeRoute) { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                            icon = { Text(stringResource(Res.string.nav_photos_icon)) },
                            label = { Text(stringResource(Res.string.nav_photos)) },
                        )
                        NavigationRailItem(
                            selected = cameraSelected,
                            enabled = !cameraSelected,
                            onClick = {
                                navController.navigate(CameraRoute) {
                                    popUpTo(HomeRoute) { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                            icon = { Text(stringResource(Res.string.nav_camera_icon)) },
                            label = { Text(stringResource(Res.string.nav_camera)) },
                        )
                        NavigationRailItem(
                            selected = mapSelected,
                            enabled = !mapSelected,
                            onClick = {
                                navController.navigate(MapRoute) {
                                    popUpTo(HomeRoute) { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                            icon = { Text(stringResource(Res.string.nav_map_icon)) },
                            label = { Text(stringResource(Res.string.nav_map)) },
                        )
                    }
                    NavHost(
                        navController = navController,
                        startDestination = HomeRoute,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        composable<HomeRoute> { HomeScreen() }
                        composable<PhotosRoute> { PhotoScreen() }
                        composable<CameraRoute> { CameraScreen() }
                        composable<MapRoute> { MapScreen() }
                    }
                }
            } else {
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            val homeSelected = currentDestination?.hasRoute<HomeRoute>() == true
                            val photosSelected = currentDestination?.hasRoute<PhotosRoute>() == true
                            val mapSelected = currentDestination?.hasRoute<MapRoute>() == true
                            val cameraSelected = currentDestination?.hasRoute<CameraRoute>() == true
                            NavigationBarItem(
                                selected = homeSelected,
                                enabled = !homeSelected,
                                onClick = {
                                    navController.navigate(HomeRoute) {
                                        popUpTo(HomeRoute) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                },
                                icon = { Text(stringResource(Res.string.nav_home_icon)) },
                                label = { Text(stringResource(Res.string.nav_home)) },
                            )
                            NavigationBarItem(
                                selected = photosSelected,
                                enabled = !photosSelected,
                                onClick = {
                                    navController.navigate(PhotosRoute) {
                                        popUpTo(HomeRoute) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                },
                                icon = { Text(stringResource(Res.string.nav_photos_icon)) },
                                label = { Text(stringResource(Res.string.nav_photos)) },
                            )
                            NavigationBarItem(
                                selected = cameraSelected,
                                enabled = !cameraSelected,
                                onClick = {
                                    navController.navigate(CameraRoute) {
                                        popUpTo(HomeRoute) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                },
                                icon = { Text(stringResource(Res.string.nav_camera_icon)) },
                                label = { Text(stringResource(Res.string.nav_camera)) },
                            )
                            NavigationBarItem(
                                selected = mapSelected,
                                enabled = !mapSelected,
                                onClick = {
                                    navController.navigate(MapRoute) {
                                        popUpTo(HomeRoute) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                },
                                icon = { Text(stringResource(Res.string.nav_map_icon)) },
                                label = { Text(stringResource(Res.string.nav_map)) },
                            )
                        }
                    },
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = HomeRoute,
                        modifier = Modifier.padding(innerPadding),
                    ) {
                        composable<HomeRoute> { HomeScreen() }
                        composable<PhotosRoute> { PhotoScreen() }
                        composable<CameraRoute> { CameraScreen() }
                        composable<MapRoute> { MapScreen() }
                    }
                }
            }
        }
    }
}
