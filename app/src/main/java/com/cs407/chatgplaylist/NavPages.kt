package com.cs407.chatgplaylist

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
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var userState by remember { mutableStateOf<UserState?>(null) }
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
            LoginPage() { newUser ->
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
            val model = GenerativeModel(
                modelName = "gemini-2.0-flash",
                apiKey = "AIzaSyB8iOC4iY171dHIXznqJy3L97Xp_spMEgc"
            )

            LaunchedEffect(playlistId) {
                val playlistDao = db.playlistDao()
                val prompt = navController
                    .getBackStackEntry("upload")
                    .savedStateHandle
                    .get<String>("prompt")
                    .orEmpty()
                val finalPrompt = """
                    Create a playlist of about 10 songs based on the following description:
                    "$prompt"
                    Return ONLY a plain text list of songs, one per line. Each line must be in the exact format: Song name - Artist
                    Do not include numbers, bullet points, quotes, extra text, or explanations."""
                    .trimIndent()

                val rawResponse = if (prompt.isNotBlank()) {
                    try {
                        val result = withContext(Dispatchers.IO) {
                            model.generateContent(finalPrompt)
                        }
                        result.text ?: ""
                    } catch (e: Exception) {
                        "Error generating playlist: ${e.message}"
                    }
                } else {
                    "No prompt provided."
                }

                val songs = rawResponse
                    .lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                val accessToken = SpotifyAuth.currentAccessToken()

                withContext(Dispatchers.IO) {
                    val songsToInsert = songs.map { line ->
                        val parts = line.split("-", limit = 2)
                        val title = parts.getOrNull(0)?.trim().orEmpty()
                        val artist = parts.getOrNull(1)?.trim().orEmpty()

                        var song = Song(
                            songId = 0,
                            playlistId = playlistId,
                            title = title,
                            artist = artist
                        )

                        if (!accessToken.isNullOrEmpty() && title.isNotBlank()) {
                            val match = runCatching {
                                SpotifySearch.searchTrack(accessToken, title, artist)
                            }.getOrNull()
                            if (match != null) {
                                song = song.copy(
                                    spotifyUri = match.uri,
                                    spotifyUrl = match.url
                                )
                            }
                        }
                        song
                    }
                    if (songsToInsert.isNotEmpty()) {
                        playlistDao.insertSongs(songsToInsert)
                    }
                }

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
