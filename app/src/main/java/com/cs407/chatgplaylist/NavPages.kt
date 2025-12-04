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
import com.cs407.chatgplaylist.spotify.SpotifyDemo

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
            LoadingScreen()

            val context = LocalContext.current
            val db = remember { PlaylistDatabase.getDatabase(context) }
            val model = GenerativeModel(
                modelName = "gemini-2.0-flash",
                apiKey = "AIzaSyB8iOC4iY171dHIXznqJy3L97Xp_spMEgc"
            )

            LaunchedEffect(playlistId) {
                val playlistDao = db.playlistDao()
                val uploadEntry = navController
                    .getBackStackEntry("upload")
                val historyAccessToken = SpotifyAuth.currentAccessToken()

                val recentSongs = if (!historyAccessToken.isNullOrEmpty()) {
                    try {
                        SpotifyDemo.fetchRecentTrackNames(historyAccessToken, limit = 20)
                    } catch (e: Exception) {
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                val prompt = uploadEntry
                    .savedStateHandle
                    .get<String>("prompt")
                    .orEmpty()

                val imageUri = uploadEntry
                    .savedStateHandle
                    .get<String>("imageUri")

                val bitmap: Bitmap? = imageUri?.let { uri ->
                    val finalUri = uri.toUri()
                    val source = ImageDecoder.createSource(context.contentResolver, finalUri)
                    ImageDecoder.decodeBitmap(source)
                }

                val historyClause = if (recentSongs.isNotEmpty()) {
                    "Here are some of the user's recently played songs for additional context: " + recentSongs.joinToString(separator = "; ") + ". Use this as a reference when generating new playlists, but do not include any of the songs listed here in the generated playlists."
                } else {
                    ""
                }

                val finalPromptBoth = """
                    Create a playlist of no more than 10 songs based on the following description:
                    "$prompt" and the image attached.
                    $historyClause
                    Return only a list of songs. The format is one per line, and each line must be in the exact format of "Song name - Artist".
                    Do not include numbers, bullet points, quotes, extra text, or explanations."""
                    .trimIndent()
                val finalPromptImageOnly = """
                    Create a playlist of no more than 10 songs based on the image attached.
                    $historyClause
                    Return only a list of songs. The format is one per line, and each line must be in the exact format of "Song name - Artist".
                    Do not include numbers, bullet points, quotes, extra text, or explanations."""
                    .trimIndent()
                val finalPromptTextOnly = """
                    Create a playlist of no more than 10 songs based on the following description:
                    "$prompt"
                    $historyClause
                    Return only a list of songs. The format is one per line, and each line must be in the exact format of "Song name - Artist".
                    Do not include numbers, bullet points, quotes, extra text, or explanations."""
                    .trimIndent()
                val finalPromptHistoryOnly = """
                    Create a playlist of no more than 10 songs based on the following description:
                    $historyClause
                    Return only a list of songs. The format is one per line, and each line must be in the exact format of "Song name - Artist".
                    Do not include numbers, bullet points, quotes, extra text, or explanations."""
                    .trimIndent()

                val rawResponse = if (prompt.isNotBlank() || bitmap != null || recentSongs.isNotEmpty()) {
                    try {
                        val result = withContext(Dispatchers.IO) {
                            if (bitmap != null && prompt.isNotBlank()) {
                                val input = content {
                                    image(bitmap)
                                    text(finalPromptBoth)
                                }
                                model.generateContent(input)
                            } else {
                                if (bitmap != null && !prompt.isNotBlank()) {
                                    val input = content {
                                        image(bitmap)
                                        text(finalPromptImageOnly)
                                    }
                                    model.generateContent(input)
                                }
                                else {
                                    if (bitmap == null && prompt.isNotBlank()) {
                                        model.generateContent(finalPromptTextOnly)
                                    }
                                    else {
                                        model.generateContent(finalPromptHistoryOnly)
                                    }
                                }
                            }
                        }
                        result.text ?: ""
                    } catch (e: Exception) {
                        e.message.toString()
                    }
                } else {
                    "You should enter some information to generate playlists."
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
