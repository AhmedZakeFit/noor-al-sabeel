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
import androidx.lifecycle.viewmodel.compose.viewModel
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
import io.github.ahmedzakefit.noor.ui.update.UpdateFlowHost
import io.github.ahmedzakefit.noor.ui.update.UpdateViewModel

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
                val context = androidx.compose.ui.platform.LocalContext.current
                val activityStoreOwner = (context as? androidx.lifecycle.ViewModelStoreOwner)
                    ?: androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner.current
                val updateVm: UpdateViewModel = viewModel(
                    viewModelStoreOwner = checkNotNull(activityStoreOwner),
                    factory = UpdateViewModel.Factory
                )

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            updateVm = updateVm,
                            onOpenLocationPicker = { navController.navigate("settings") },
                            onRequestNotificationPermission = onRequestNotificationPermission,
                        )
                    }
                    composable("qibla") { QiblaScreen() }
                    composable("mosques") { MosquesScreen() }
                    composable("settings") {
                        SettingsScreen(
                            updateVm = updateVm,
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
                UpdateFlowHost(updateVm)
            }
        }
    }
}

@Composable
private fun NoorBottomBar(navController: NavHostController, destinations: List<BottomDestination>) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    androidx.compose.material3.NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = androidx.compose.ui.unit.dp(8),
    ) {
        destinations.forEach { dest ->
            val isSelected = currentRoute == dest.route
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    navController.navigate(dest.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    if (dest.iconRes != null) {
                        Icon(
                            painterResource(dest.iconRes),
                            contentDescription = stringResource(dest.labelRes),
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Icon(
                            dest.vectorIcon!!,
                            contentDescription = stringResource(dest.labelRes),
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                label = {
                    Text(
                        stringResource(dest.labelRes),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            )
        }
    }
}
