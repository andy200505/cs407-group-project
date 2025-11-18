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
                userState?.let {
                    UploadScreen(navController, it)
                }
            }

            composable("playlist") {
                userState?.let { PlaylistScreen(navController) }
            }

            composable("profile") {
                userState?.let { ProfileScreen(navController, it) }
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
