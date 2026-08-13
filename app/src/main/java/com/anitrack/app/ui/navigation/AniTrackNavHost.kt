package com.anitrack.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onAnimeClick = { animeId ->
                    navController.navigate(Screen.Detail.createRoute(animeId))
                }
            )
        }
        
        composable(Screen.Search.route) {
            SearchScreen(
                onAnimeClick = { animeId ->
                    navController.navigate(Screen.Detail.createRoute(animeId))
                }
            )
        }
        
        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onAnimeClick = { animeId ->
                    navController.navigate(Screen.Detail.createRoute(animeId))
                }
            )
        }
        
        composable(Screen.Profile.route) {
            ProfileScreen()
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
