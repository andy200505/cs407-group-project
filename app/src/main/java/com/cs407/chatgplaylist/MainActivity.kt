package com.cs407.chatgplaylist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.cs407.chatgplaylist.auth.SpotifyAuth
import com.cs407.chatgplaylist.data.PlaylistDatabase
import com.cs407.chatgplaylist.data.PlaylistDao
import com.cs407.chatgplaylist.data.Song
import com.cs407.chatgplaylist.data.ThemePreferences
import com.cs407.chatgplaylist.spotify.SpotifyDemo
import com.cs407.chatgplaylist.spotify.SpotifySearch
import com.cs407.chatgplaylist.ui.theme.ChatGPlaylisTTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    companion object {
        val spotifyConnected = mutableStateOf(false)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        spotifyConnected.value = !SpotifyAuth.currentAccessToken().isNullOrEmpty()
        setContent {
            val context = this
            val darkModeEnabled by ThemePreferences.getThemeFlow(context)
                .collectAsState(initial = false)

            //makes battery and wifi symbol light when in dark mode
            val insetsController = androidx.core.view.WindowCompat
                .getInsetsController(window, window.decorView)
            insetsController.isAppearanceLightStatusBars = !darkModeEnabled

            ChatGPlaylisTTheme(darkTheme = darkModeEnabled) {
                AppNavigation(
                    darkMode = darkModeEnabled,
                    onToggleDarkMode = {
                        lifecycleScope.launch {
                            ThemePreferences.setDarkMode(context, !darkModeEnabled)
                        }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (SpotifyAuth.isSpotifyRedirect(intent)) {
            SpotifyAuth.handleRedirect(intent) { result ->
                runOnUiThread {
                    when (result) {
                        is SpotifyAuth.Result.Success -> {
                            Log.d("SpotifyAuth", "Access token received")
                            spotifyConnected.value = true
                            Toast.makeText(this, "Spotify connected", Toast.LENGTH_SHORT).show()
                        }
                        is SpotifyAuth.Result.Error -> {
                            Log.w("SpotifyAuth", "Auth failed: ${result.message}")
                            Toast.makeText(
                                this,
                                "Spotify login failed: ${result.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }

    fun startSpotifyAuth() {
        val clientId = getString(R.string.spotify_client_id)
        val redirect = getString(R.string.spotify_redirect_uri)
        SpotifyAuth.startSignIn(this, clientId, redirect)
    }

    fun createDemoPlaylist(playlistId: Int? = null) {
        val token = SpotifyAuth.currentAccessToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Connect Spotify first.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val result = if (playlistId == null) {
                SpotifyDemo.createSamplePlaylist(token)
            } else {
                createPlaylistFromDb(token, playlistId)
            }
            when (result) {
                is SpotifyDemo.Result.Success -> Toast.makeText(
                    this@MainActivity,
                    "Playlist \"${result.playlistName}\" created.",
                    Toast.LENGTH_LONG
                ).show()
                is SpotifyDemo.Result.Error -> Toast.makeText(
                    this@MainActivity,
                    "Spotify error: ${result.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private suspend fun createPlaylistFromDb(
        token: String,
        playlistId: Int
    ): SpotifyDemo.Result {
        return withContext(Dispatchers.IO) {
            val db = PlaylistDatabase.getDatabase(applicationContext)
            val playlistDao = db.playlistDao()
            val songs = playlistDao.getSongsForPlaylist(playlistId)
            val hydrated = ensureSpotifyMatches(token, songs, playlistDao)
            SpotifyDemo.createPlaylistFromSongs(token, hydrated)
        }
    }

    private suspend fun ensureSpotifyMatches(
        token: String,
        songs: List<Song>,
        playlistDao: PlaylistDao
    ): List<Song> {
        if (songs.isEmpty()) return songs
        val updatedSongs = mutableListOf<Song>()
        val changed = mutableListOf<Song>()

        songs.forEach { song ->
            if (song.spotifyUri.isNullOrEmpty()) {
                val match = runCatching {
                    SpotifySearch.searchTrack(token, song.title, song.artist)
                }.getOrNull()
                if (match != null) {
                    val enriched = song.copy(
                        spotifyUri = match.uri,
                        spotifyUrl = match.url
                    )
                    updatedSongs += enriched
                    changed += enriched
                } else {
                    updatedSongs += song
                }
            } else {
                updatedSongs += song
            }
        }

        if (changed.isNotEmpty()) {
            playlistDao.updateSongs(changed)
        }

        return updatedSongs
    }
}
