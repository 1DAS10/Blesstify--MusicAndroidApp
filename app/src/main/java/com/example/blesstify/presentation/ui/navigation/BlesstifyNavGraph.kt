package com.example.blesstify.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.blesstify.presentation.ui.main.AIFocusScreen
import com.example.blesstify.presentation.ui.splash.SplashScreen
import com.example.blesstify.presentation.ui.login.LoginScreen
import com.example.blesstify.presentation.ui.register.RegisterScreen
import com.example.blesstify.presentation.ui.main.MainScreen
import com.example.blesstify.presentation.ui.player.PlayerScreen
import com.example.blesstify.presentation.ui.playlist.PlaylistDetailScreen
import com.example.blesstify.presentation.ui.playlist.CreatePlaylistScreen
import com.example.blesstify.presentation.ui.history.ListeningHistoryScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.blesstify.domain.auth.AuthState
import com.example.blesstify.presentation.auth.AuthUiEvent
import com.example.blesstify.presentation.auth.AuthViewModel
import com.example.blesstify.presentation.player.PlayerViewModel

@Composable
fun BlessifyNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Shared PlayerViewModel — MusicController is a singleton so state is preserved across screens
    val playerViewModel: PlayerViewModel = hiltViewModel()

    LaunchedEffect(authState.authState, currentRoute) {
        when (authState.authState) {
            is AuthState.Authenticated -> {
                // Only redirect to Main from auth/splash screens, NOT from sub-screens
                if (currentRoute == Screen.Splash.route ||
                    currentRoute == Screen.Login.route ||
                    currentRoute == Screen.Register.route
                ) {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            AuthState.Unauthenticated -> {
                if (currentRoute == Screen.Splash.route || currentRoute == Screen.Main.route) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            AuthState.Unknown -> Unit
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onGetStarted = { navController.navigate(Screen.Login.route) }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToMain = { navController.navigate(Screen.Main.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                    launchSingleTop = true
                }}
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Main.route) {
            MainScreen(
                playerViewModel = playerViewModel,
                onNavigateToPlayer = { song ->
                    playerViewModel.playSong(song)
                    navController.navigate(Screen.Player.route)
                },
                onNavigateToPlayerFromSearch = { songs, startIndex ->
                    playerViewModel.playFromList(songs, startIndex, source = "search")
                    navController.navigate(Screen.Player.route)
                },
                onNavigateToPlaylist = { playlistId -> navController.navigate(Screen.PlaylistDetail.createRoute(playlistId)) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToEqualizer = { navController.navigate(Screen.Equalizer.route) },
                onNavigateToUpload = { navController.navigate(Screen.UploadMusic.route) },
                onNavigateToCreatePlaylist = { navController.navigate(Screen.CreatePlaylist.route) },
                onNavigateToAIFocus = { navController.navigate(Screen.AIFocus.route) },
                onNavigateToNotification = { navController.navigate(Screen.Notification.route) },
                onNavigateToCategory = { genre -> navController.navigate(Screen.CategoryDetail.createRoute(genre)) },
                onOpenPlayer = { navController.navigate(Screen.Player.route) },
                onLogout = { authViewModel.onEvent(AuthUiEvent.SignOut) }
            )
        }
        composable(Screen.Notification.route) {
            com.example.blesstify.presentation.ui.notification.NotificationScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPlaylist = { id -> navController.navigate(Screen.PlaylistDetail.createRoute(id)) },
                onNavigateToPlayer = { id -> 
                    navController.navigate(Screen.Player.route)
                },
                onNavigateToAIFocus = { navController.navigate(Screen.AIFocus.route) },
                onNavigateToProfile = { 
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Player.route) {
            PlayerScreen(
                onBack = { navController.popBackStack() },
                viewModel = playerViewModel
            )
        }
        composable(
            route = Screen.PlaylistDetail.route,
            arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
        ) {
            PlaylistDetailScreen(
                onBack = { navController.popBackStack() },
                onPlay = { navController.navigate(Screen.Player.route) }
            )
        }
        composable(Screen.CreatePlaylist.route) {
            CreatePlaylistScreen(
                onClose = { navController.popBackStack() }
            )
        }
        composable(Screen.AIFocus.route) {
            AIFocusScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.History.route) {
            ListeningHistoryScreen(
                onBackClick = { navController.popBackStack() },
                onPlayerClick = { navController.navigate(Screen.Player.route) },
                playerViewModel = playerViewModel
            )
        }
        composable(Screen.Equalizer.route) {
            com.example.blesstify.presentation.ui.profile.EqualizerScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.UploadMusic.route) {
            com.example.blesstify.presentation.ui.library.UploadMusicScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.CategoryDetail.route,
            arguments = listOf(navArgument("genre") { type = NavType.StringType })
        ) { backStackEntry ->
            val genre = android.net.Uri.decode(backStackEntry.arguments?.getString("genre") ?: "")
            com.example.blesstify.presentation.ui.main.CategoryDetailScreen(
                genre = genre,
                onBack = { navController.popBackStack() },
                onNavigateToPlayer = { songs, startIndex ->
                    playerViewModel.playFromList(songs, startIndex, source = "category")
                    navController.navigate(Screen.Player.route)
                }
            )
        }
    }
}
