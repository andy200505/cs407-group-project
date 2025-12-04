package com.cs407.chatgplaylist

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cs407.chatgplaylist.auth.SpotifyAuth
import com.cs407.chatgplaylist.data.PlaylistDatabase
import com.cs407.chatgplaylist.data.Song
import com.cs407.chatgplaylist.data.User
import com.cs407.chatgplaylist.data.UserState
import com.cs407.chatgplaylist.spotify.SpotifySearch
import com.cs407.chatgplaylist.ui.theme.screens.LoadingScreen
import com.cs407.chatgplaylist.ui.theme.screens.LoginPage
import com.cs407.chatgplaylist.ui.theme.screens.PlaylistScreen
import com.cs407.chatgplaylist.ui.theme.screens.ProfileScreen
import com.cs407.chatgplaylist.ui.theme.screens.UploadScreen
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs407.chatgplaylist.spotify.SpotifyDemo
import com.cs407.chatgplaylist.viewmodels.GeneratingViewModel
import androidx.compose.animation.ExperimentalAnimationApi

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var userState by remember { mutableStateOf<UserState?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val db = PlaylistDatabase.getDatabase(context)

    LaunchedEffect(Unit) {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            val roomUser = withContext(Dispatchers.IO) {
                getOrCreateRoomUser(currentUser.uid, db)
            }
            userState = UserState(roomUser.userId, currentUser.email ?: "")
        }
        isLoading = false
    }

    val startDestination = if (Firebase.auth.currentUser != null) "upload" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginPage(
                onLoginComplete = { newUser ->
                    userState = newUser
                    navController.navigate("upload") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("upload") {
            userState?.let { UploadScreen(navController, it) }
        }

        composable("playlist/{playlistId}") { backStackEntry ->
            val playlistId = backStackEntry.arguments!!.getString("playlistId")!!.toInt()
            PlaylistScreen(navController, playlistId)
        }

        composable("profile") {
            userState?.let { ProfileScreen(navController, it) }
        }

        composable("loading/{playlistId}") { backStackEntry ->
            val playlistId = backStackEntry.arguments!!.getString("playlistId")!!.toInt()
            val GeneratingViewModel: GeneratingViewModel = viewModel()

            val uploadEntry = navController.getBackStackEntry("upload")
            val prompt = uploadEntry.savedStateHandle.get<String>("prompt").orEmpty()
            val imageUri = uploadEntry.savedStateHandle.get<String>("imageUri")

            LaunchedEffect(playlistId) {
                GeneratingViewModel.generatePlaylistFor(
                    playlistId = playlistId,
                    prompt = prompt,
                    imageUriString = imageUri
                )
            }

            LaunchedEffect(GeneratingViewModel.isDone) {
                if (GeneratingViewModel.isDone) {
                    navController.navigate("playlist/$playlistId") {
                        popUpTo("upload") { inclusive = false }
                    }
                }
            }

            LoadingScreen()
        }
    }
}

//helper function to get or create user
suspend fun getOrCreateRoomUser(uid: String, db: PlaylistDatabase): User {
    val existingUser = db.userDao().getByUID(uid)
    return existingUser ?: run {
        val newUser = User(userUID = uid)
        db.userDao().insert(newUser)
        db.userDao().getByUID(uid)!! //retrieve after insert
    }
}
