package io.github.ahmedzakefit.noor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.ahmedzakefit.noor.R
import io.github.ahmedzakefit.noor.appContainer
import io.github.ahmedzakefit.noor.data.AppSettings
import io.github.ahmedzakefit.noor.ui.home.HomeScreen
import io.github.ahmedzakefit.noor.ui.mosques.MosquesScreen
import io.github.ahmedzakefit.noor.ui.qibla.QiblaScreen
import io.github.ahmedzakefit.noor.ui.settings.SettingsScreen
import io.github.ahmedzakefit.noor.ui.theme.NOORTheme

private data class BottomDestination(
    val route: String,
    val iconRes: Int?,
    val vectorIcon: ImageVector?,
    val labelRes: Int,
)

@Composable
fun NoorApp(
    onRequestNotificationPermission: () -> Unit,
) {
    val container = androidx.compose.ui.platform.LocalContext.current.appContainer
    val settings by container.settingsRepository.settings
        .collectAsStateWithLifecycle(initialValue = AppSettings())

    NOORTheme(themeSetting = settings.theme) {
        val navController = rememberNavController()
        val destinations = listOf(
            BottomDestination("home", R.drawable.ic_notification, null, R.string.nav_times),
            BottomDestination("qibla", R.drawable.ic_compass, null, R.string.nav_qibla),
            BottomDestination("mosques", null, Icons.Filled.Place, R.string.nav_mosques),
            BottomDestination("settings", null, Icons.Filled.Settings, R.string.nav_settings),
        )
        Scaffold(
            bottomBar = { NoorBottomBar(navController, destinations) },
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            onOpenLocationPicker = { navController.navigate("settings") },
                            onRequestNotificationPermission = onRequestNotificationPermission,
                        )
                    }
                    composable("qibla") { QiblaScreen() }
                    composable("mosques") { MosquesScreen() }
                    composable("settings") {
                        SettingsScreen(
                            onFirstLocationSet = {
                                navController.navigate("home") {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoorBottomBar(navController: NavHostController, destinations: List<BottomDestination>) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    NavigationBar {
        destinations.forEach { dest ->
            NavigationBarItem(
                selected = currentRoute == dest.route,
                onClick = {
                    navController.navigate(dest.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    if (dest.iconRes != null) {
                        Icon(painterResource(dest.iconRes), contentDescription = stringResource(dest.labelRes))
                    } else {
                        Icon(dest.vectorIcon!!, contentDescription = stringResource(dest.labelRes))
                    }
                },
                label = { Text(stringResource(dest.labelRes)) },
            )
        }
    }
}
