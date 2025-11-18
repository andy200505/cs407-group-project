package com.cs407.chatgplaylist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cs407.chatgplaylist.data.PlaylistDatabase
import com.cs407.chatgplaylist.data.User
import com.cs407.chatgplaylist.data.UserState
import com.cs407.chatgplaylist.ui.theme.screens.LoadingScreen
import com.cs407.chatgplaylist.ui.theme.screens.LoginPage
import com.cs407.chatgplaylist.ui.theme.screens.PlaylistScreen
import com.cs407.chatgplaylist.ui.theme.screens.ProfileScreen
import com.cs407.chatgplaylist.ui.theme.screens.UploadScreen
import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var userState by remember { mutableStateOf<UserState?>(null) }
    //to be used when playlist is generating
    var isLoading by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val db = PlaylistDatabase.getDatabase(context)

    //init userState if Firebase user exists
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
            LoginPage(navController) { newUser ->
                userState = newUser
                navController.navigate("upload") {
                    popUpTo("login") { inclusive = true }
                }
            }
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
            LoadingScreen()
            val context = LocalContext.current
            val db = remember { PlaylistDatabase.getDatabase(context) }

            LaunchedEffect(playlistId) {
                val playlistDao = db.playlistDao()

                // TODO: call backend AI to generate songs
                // use isLoading to wait for the playlist to be generated until
                // navigating to PlaylistScreen. Navigating before then will
                // cause the playlist to appear empty

                //once ready, go to PlaylistScreen
                navController.navigate("playlist/$playlistId") {
                    popUpTo("upload") { inclusive = false }
                }
            }
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
