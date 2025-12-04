package com.cs407.chatgplaylist.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.chatgplaylist.auth.SpotifyAuth
import com.cs407.chatgplaylist.data.PlaylistDatabase
import com.cs407.chatgplaylist.data.Song
import com.cs407.chatgplaylist.spotify.SpotifyDemo
import com.cs407.chatgplaylist.spotify.SpotifySearch
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GeneratingViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PlaylistDatabase.getDatabase(application)
    private val playlistDao = db.playlistDao()

    // Simple state so the UI (NavHost) knows when to navigate away
    var isDone by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val model = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = "AIzaSyB8iOC4iY171dHIXznqJy3L97Xp_spMEgc"
    )

    /**
     * Same behavior as your original NavPages loading route:
     *  - Reads prompt + (optional) image
     *  - Optionally uses listening history
     *  - Calls Gemini to generate songs
     *  - Enriches via SpotifySearch when possible
     *  - Inserts songs into the given playlist
     */
    fun generatePlaylistFor(
        playlistId: Int,
        prompt: String,
        imageUriString: String?
    ) {
        // Avoid running multiple times if the composable recomposes
        if (isDone) return

        isDone = false
        errorMessage = null

        viewModelScope.launch {
            val app = getApplication<Application>()

            // Decode bitmap from the URI (if provided)
            val bitmap: Bitmap? = imageUriString?.let { uriStr ->
                try {
                    val uri = uriStr.toUri()
                    val source = ImageDecoder.createSource(app.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } catch (_: Exception) {
                    null
                }
            }

            // Recently played tracks via Spotify
            val historyAccessToken = SpotifyAuth.currentAccessToken()
            val recentSongs = if (!historyAccessToken.isNullOrEmpty()) {
                try {
                    SpotifyDemo.fetchRecentTrackNames(historyAccessToken, limit = 20)
                } catch (_: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }

            val historyClause = if (recentSongs.isNotEmpty()) {
                // This matches your original string (with the "..." included)
                "Here are some of the user's recently played songs for additional context: " + recentSongs.joinToString(separator = "; ") + ". Use this as a reference when generating new playlists, but do not include any of the songs listed here in the generated playlists."
            } else {
                ""
            }

            val finalPromptBoth = """
                    Create a playlist of no more than 10 songs based on the following description:
                    "$prompt" and the image attached.
                    $historyClause
                    Return only a list of songs. The format... each line must be in the exact format of "Song name - Artist".
                    Do not include numbers, bullet points, quotes, extra text, or explanations."""
                .trimIndent()

            val finalPromptImageOnly = """
                    Create a playlist of no more than 10 songs based on the image attached.
                    $historyClause
                    Return only a list of songs. The format... each line must be in the exact format of "Song name - Artist".
                    Do not include numbers, bullet points, quotes, extra text, or explanations."""
                .trimIndent()

            val finalPromptTextOnly = """
                    Create a playlist of no more than 10 songs based on the following description:
                    "$prompt"
                    $historyClause
                    Return only a list of songs. The format... each line must be in the exact format of "Song name - Artist".
                    Do not include numbers, bullet points, quotes, extra text, or explanations."""
                .trimIndent()

            val finalPromptHistoryOnly = """
                    Create a playlist of no more than 10 songs based on the following description:
                    $historyClause
                    Return only a list of songs. The format... each line must be in the exact format of "Song name - Artist".
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

            // Insert songs into Room, enriching with Spotify metadata when possible
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

            // Let the UI know we're done so it can navigate to the playlist screen
            isDone = true
        }
    }
}