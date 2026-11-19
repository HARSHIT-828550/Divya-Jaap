package com.example.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.DivyaJaapViewModelFactory
import com.example.presentation.counter.JaapCounterScreen
import com.example.presentation.counter.JaapCounterViewModel
import com.example.presentation.dashboard.DashboardScreen
import com.example.presentation.dashboard.DashboardViewModel
import com.example.presentation.guru.MantraGuruScreen
import com.example.presentation.guru.MantraGuruViewModel
import com.example.presentation.library.MantraLibraryScreen
import com.example.presentation.library.MantraLibraryViewModel
import com.example.presentation.practice.PronunciationPracticeScreen
import com.example.presentation.practice.PronunciationPracticeViewModel
import com.example.presentation.settings.SettingsScreen
import com.example.presentation.settings.SettingsViewModel
import com.example.presentation.theme.IvoryText
import com.example.presentation.theme.NavyElevated
import com.example.presentation.theme.NavySanctum
import com.example.presentation.theme.NavyVoid
import com.example.presentation.theme.SacredGold
import com.example.presentation.theme.SacredGoldBright
import com.example.presentation.theme.SandText

object DivyaRoutes {
    const val COUNTER = "counter"
    const val GURU = "guru"
    const val PRACTICE = "practice"
    const val LIBRARY = "library"
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
}

data class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val topLevelDestinations = listOf(
    TopLevelDestination(
        route = DivyaRoutes.COUNTER,
        label = "Jaap",
        selectedIcon = Icons.Filled.SelfImprovement,
        unselectedIcon = Icons.Outlined.SelfImprovement,
        testTag = "nav_jaap"
    ),
    TopLevelDestination(
        route = DivyaRoutes.GURU,
        label = "Guru",
        selectedIcon = Icons.Filled.RecordVoiceOver,
        unselectedIcon = Icons.Outlined.RecordVoiceOver,
        testTag = "nav_guru"
    ),
    TopLevelDestination(
        route = DivyaRoutes.PRACTICE,
        label = "Practice",
        selectedIcon = Icons.Filled.Mic,
        unselectedIcon = Icons.Outlined.Mic,
        testTag = "nav_practice"
    ),
    TopLevelDestination(
        route = DivyaRoutes.LIBRARY,
        label = "Library",
        selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
        unselectedIcon = Icons.AutoMirrored.Outlined.MenuBook,
        testTag = "nav_library"
    ),
    TopLevelDestination(
        route = DivyaRoutes.DASHBOARD,
        label = "Insights",
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart,
        testTag = "nav_insights"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DivyaJaapAppShell(viewModelFactory: DivyaJaapViewModelFactory) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isOnSettings = currentDestination?.route == DivyaRoutes.SETTINGS

    val counterVm: JaapCounterViewModel = viewModel(factory = viewModelFactory)
    val guruVm: MantraGuruViewModel = viewModel(factory = viewModelFactory)
    val practiceVm: PronunciationPracticeViewModel = viewModel(factory = viewModelFactory)
    val libraryVm: MantraLibraryViewModel = viewModel(factory = viewModelFactory)
    val dashboardVm: DashboardViewModel = viewModel(factory = viewModelFactory)
    val settingsVm: SettingsViewModel = viewModel(factory = viewModelFactory)

    val navigateTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyVoid)
    ) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = NavyVoid,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = NavyVoid,
                        titleContentColor = SacredGoldBright
                    ),
                    navigationIcon = {
                        if (isOnSettings) {
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = SacredGoldBright
                                )
                            }
                        }
                    },
                    title = {
                        Text(
                            text = if (isOnSettings) "SETTINGS" else "DIVYA JAAP",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = SacredGoldBright
                            )
                        )
                    },
                    actions = {
                        if (!isOnSettings) {
                            IconButton(
                                onClick = { navController.navigate(DivyaRoutes.SETTINGS) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("nav_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Open Settings",
                                    tint = SacredGold
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!useNavigationRail && !isOnSettings) {
                    NavigationBar(
                        containerColor = NavySanctum,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        topLevelDestinations.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NavyVoid,
                                    selectedTextColor = SacredGoldBright,
                                    indicatorColor = SacredGold,
                                    unselectedIconColor = SandText,
                                    unselectedTextColor = SandText
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail && !isOnSettings) {
                    NavigationRail(
                        containerColor = NavySanctum,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        topLevelDestinations.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = NavyVoid,
                                    selectedTextColor = SacredGoldBright,
                                    indicatorColor = SacredGold,
                                    unselectedIconColor = SandText,
                                    unselectedTextColor = SandText
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = DivyaRoutes.COUNTER
                    ) {
                        composable(DivyaRoutes.COUNTER) {
                            JaapCounterScreen(
                                viewModel = counterVm,
                                onOpenLibrary = { navigateTab(DivyaRoutes.LIBRARY) },
                                onOpenGuruWithMantra = { title, text ->
                                    guruVm.loadTextAndPlay(title, text)
                                    navigateTab(DivyaRoutes.GURU)
                                }
                            )
                        }
                        composable(DivyaRoutes.GURU) {
                            MantraGuruScreen(viewModel = guruVm)
                        }
                        composable(DivyaRoutes.PRACTICE) {
                            PronunciationPracticeScreen(viewModel = practiceVm)
                        }
                        composable(DivyaRoutes.LIBRARY) {
                            MantraLibraryScreen(
                                viewModel = libraryVm,
                                onStartJaap = { navigateTab(DivyaRoutes.COUNTER) },
                                onOpenGuru = { title, text ->
                                    guruVm.loadTextAndPlay(title, text)
                                    navigateTab(DivyaRoutes.GURU)
                                },
                                onOpenPractice = { mantraId ->
                                    practiceVm.selectMantra(mantraId)
                                    navigateTab(DivyaRoutes.PRACTICE)
                                }
                            )
                        }
                        composable(DivyaRoutes.DASHBOARD) {
                            DashboardScreen(viewModel = dashboardVm)
                        }
                        composable(DivyaRoutes.SETTINGS) {
                            SettingsScreen(viewModel = settingsVm)
                        }
                    }
                }
            }
        }
    }
}
