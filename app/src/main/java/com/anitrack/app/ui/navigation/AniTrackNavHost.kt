package com.anitrack.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.anitrack.app.ui.detail.DetailScreen
import com.anitrack.app.ui.favorites.FavoritesScreen
import com.anitrack.app.ui.home.HomeScreen
import com.anitrack.app.ui.profile.ProfileScreen
import com.anitrack.app.ui.search.SearchScreen
import com.anitrack.app.ui.components.AniTrackBottomNavigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Favorites : Screen("favorites")
    object Profile : Screen("profile")
    object Detail : Screen("detail/{animeId}") {
        fun createRoute(animeId: Int) = "detail/$animeId"
    }
}

@Composable
fun AniTrackNavHost(
    navController: NavHostController = rememberNavController()
) {
    // Get current route for bottom navigation highlighting
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route
    
    // Extract base route (without arguments like "detail/123")
    val baseRoute = currentRoute.split("/")?.firstOrNull() ?: Screen.Home.route
    
    // Navigation handler for bottom bar
    val onNavigate: (String) -> Unit = { route ->
        if (route != baseRoute) {
            navController.navigate(route) {
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
    
    // Scaffold with bottom navigation
    androidx.compose.material3.Scaffold(
        bottomBar = {
            // Don't show bottom navigation on detail screen
            if (baseRoute != "detail") {
                AniTrackBottomNavigation(
                    currentRoute = baseRoute,
                    onNavigate = onNavigate
                )
            }
        }
    ) { paddingValues ->
        androidx.compose.foundation.layout.PaddingValues(
            top = paddingValues.calculateTopPadding(),
            bottom = 0,  // Bottom padding handled by Scaffold's bottomBar
            start = paddingValues.calculateStartPadding(),
            end = paddingValues.calculateEndPadding()
        )
        
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = androidx.compose.ui.Modifier.padding(
                top = paddingValues.calculateTopPadding(),
                start = paddingValues.calculateStartPadding(),
                end = paddingValues.calculateEndPadding()
            )
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onAnimeClick = { animeId ->
                        navController.navigate(Screen.Detail.createRoute(animeId))
                    },
                    onNavigate = onNavigate
                )
            }
            
            composable(Screen.Search.route) {
                SearchScreen(
                    onAnimeClick = { animeId ->
                        navController.navigate(Screen.Detail.createRoute(animeId))
                    },
                    onNavigate = onNavigate
                )
            }
            
            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    onAnimeClick = { animeId ->
                        navController.navigate(Screen.Detail.createRoute(animeId))
                    },
                    onNavigate = onNavigate
                )
            }
            
            composable(Screen.Profile.route) {
                ProfileScreen(onNavigate = onNavigate)
            }
            
            composable(Screen.Detail.route) { backStackEntry ->
                val animeId = backStackEntry.arguments?.getString("animeId")?.toIntOrNull() ?: 0
                DetailScreen(
                    animeId = animeId,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
