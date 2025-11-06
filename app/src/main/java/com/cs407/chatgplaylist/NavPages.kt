package com.cs407.chatgplaylist

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cs407.chatgplaylist.ui.theme.screens.PlaylistScreen
import com.cs407.chatgplaylist.ui.theme.screens.ProfileScreen
import com.cs407.chatgplaylist.ui.theme.screens.UploadScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "upload") {
        composable("upload") { UploadScreen(navController) }
        composable("playlist") { PlaylistScreen(navController) }
        composable("profile") { ProfileScreen(navController) }
    }
}