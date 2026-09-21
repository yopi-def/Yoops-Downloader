package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.domain.model.DownloadStatus
import com.example.domain.model.ThemeMode
import com.example.ui.components.MoriBottomBar
import com.example.ui.components.MoriGridBackground
import com.example.ui.components.MoriHeader
import com.example.ui.navigation.Screen
import com.example.ui.screens.downloads.DownloadsScreen
import com.example.ui.screens.downloads.DownloadsViewModel
import com.example.ui.screens.history.HistoryScreen
import com.example.ui.screens.history.HistoryViewModel
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.theme.MoriTheme

class MainActivity : ComponentActivity() {

    private var sharedLinkFromIntent: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            val settingsDataStore = MoriApplication.instance.settingsDataStore
            val settings by settingsDataStore.settings.collectAsState()

            val homeViewModel: HomeViewModel = viewModel()
            val downloadsViewModel: DownloadsViewModel = viewModel()
            val historyViewModel: HistoryViewModel = viewModel()
            val settingsViewModel: SettingsViewModel = viewModel()

            // Request Notification Permission on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* Handle result gracefully */ }

                LaunchedEffect(Unit) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            // Handle Shared Media URL
            LaunchedEffect(sharedLinkFromIntent) {
                sharedLinkFromIntent?.let { link ->
                    homeViewModel.onUrlChanged(link)
                    homeViewModel.analyzeUrl()
                    sharedLinkFromIntent = null
                }
            }

            MoriTheme(themeMode = settings.themeMode) {
                MoriAppContent(
                    homeViewModel = homeViewModel,
                    downloadsViewModel = downloadsViewModel,
                    historyViewModel = historyViewModel,
                    settingsViewModel = settingsViewModel,
                    currentTheme = settings.themeMode,
                    onToggleTheme = {
                        val next = when (settings.themeMode) {
                            ThemeMode.DARK -> ThemeMode.LIGHT
                            ThemeMode.LIGHT -> ThemeMode.DARK
                            ThemeMode.SYSTEM -> ThemeMode.DARK
                        }
                        settingsDataStore.updateTheme(next)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!text.isNullOrBlank()) {
                // Extract URL if surrounded by extra text
                val urlRegex = "(https?://\\S+)".toRegex()
                val match = urlRegex.find(text)
                sharedLinkFromIntent = match?.value ?: text.trim()
            }
        }
    }
}

@Composable
fun MoriAppContent(
    homeViewModel: HomeViewModel,
    downloadsViewModel: DownloadsViewModel,
    historyViewModel: HistoryViewModel,
    settingsViewModel: SettingsViewModel,
    currentTheme: ThemeMode,
    onToggleTheme: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    val tasks by downloadsViewModel.tasks.collectAsState()
    val activeCount = tasks.count {
        it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED
    }

    val snackbarHostState = remember { SnackbarHostState() }

    MoriGridBackground {
        Scaffold(
            topBar = {
                MoriHeader(
                    currentTheme = currentTheme,
                    onToggleTheme = onToggleTheme,
                    title = "MORI Downloader",
                    subtitle = when (currentRoute) {
                        Screen.Home.route -> "Multi-platform media extractor"
                        Screen.Downloads.route -> "Status proses & antrean unduhan"
                        Screen.History.route -> "Koleksi media yang telah tersimpan"
                        Screen.Settings.route -> "Konfigurasi engine & backend scraper"
                        else -> "Pengunduh media multi-platform"
                    }
                )
            },
            bottomBar = {
                MoriBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { screen ->
                        if (currentRoute != screen.route) {
                            navController.navigate(screen.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    activeDownloadsCount = activeCount
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        viewModel = homeViewModel,
                        snackbarHostState = snackbarHostState,
                        onNavigateToDownloads = {
                            navController.navigate(Screen.Downloads.route)
                        }
                    )
                }
                composable(Screen.Downloads.route) {
                    DownloadsScreen(
                        viewModel = downloadsViewModel
                    )
                }
                composable(Screen.History.route) {
                    HistoryScreen(
                        viewModel = historyViewModel,
                        snackbarHostState = snackbarHostState
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = settingsViewModel
                    )
                }
            }
        }
    }
}
