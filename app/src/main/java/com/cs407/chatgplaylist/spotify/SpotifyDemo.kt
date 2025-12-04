package com.cs407.chatgplaylist.spotify

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object SpotifyDemo {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val sampleTrackUris = listOf(
        "spotify:track:3hql0mbMMRqo3V3XfrTu73", // Sleep Token - The Summoning
        "spotify:track:4e9eGQYsOiBcftrWXwsVco", // System of a Down - Aerials
        "spotify:track:0VjIjW4GlUZAMYd2vXMi3b", // Blinding Lights
        "spotify:track:7ouMYWpwJ422jRcDASZB7P", // Knights of Cydonia
        "spotify:track:01apQz7E72krU1k1b4VWs7" // It's raining Tacos
    )

    sealed class Result {
        data class Success(val playlistName: String) : Result()
        data class Error(val message: String) : Result()
    }

    suspend fun createSamplePlaylist(accessToken: String): Result =
        withContext(Dispatchers.IO) {
            try {
                val user = fetchCurrentUser(accessToken)
                val playlist = createPlaylist(accessToken, user.id)
                addTracks(accessToken, playlist.id)
                Result.Success(playlist.name)
            } catch (io: IOException) {
                Result.Error(io.message ?: "Network error.")
            }
        }

    suspend fun createPlaylistFromSongs(
        accessToken: String,
        songs: List<com.cs407.chatgplaylist.data.Song>
    ): Result = withContext(Dispatchers.IO) {
        val trackUris = songs.mapNotNull { it.spotifyUri }.distinct()
        if (trackUris.isEmpty()) {
            return@withContext Result.Error("No Spotify tracks available for this playlist.")
        }
        try {
            val user = fetchCurrentUser(accessToken)
            val playlist = createPlaylist(accessToken, user.id)
            addTracks(accessToken, playlist.id, trackUris)
            Result.Success(playlist.name)
        } catch (io: IOException) {
            Result.Error(io.message ?: "Spotify error.")
        }
    }

    private data class Playlist(val id: String, val name: String)
    private data class User(val id: String)

    private fun fetchCurrentUser(token: String): User {
        val request = Request.Builder()
            .url("https://api.spotify.com/v1/me")
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Profile request failed: ${response.code} ${response.body?.string()}")
            }
            val body = response.body?.string().orEmpty()
            val json = JSONObject(body)
            val id = json.optString("id")
            if (id.isNullOrEmpty()) throw IOException("Profile response missing id.")
            return User(id)
        }
    }

    private fun createPlaylist(token: String, userId: String): Playlist {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }
        val timestamp = formatter.format(Date())
        val payload = JSONObject().apply {
            put("name", "ChatGPlaylisT Demo $timestamp")
            put("description", "Demo playlist from ChatGPlaylisT at $timestamp")
            put("public", false)
        }

        val request = Request.Builder()
            .url("https://api.spotify.com/v1/users/$userId/playlists")
            .addHeader("Authorization", "Bearer $token")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Playlist creation failed: ${response.code} ${response.body?.string()}")
            }
            val json = JSONObject(response.body?.string().orEmpty())
            val id = json.optString("id")
            val name = json.optString("name")
            if (id.isNullOrEmpty() || name.isNullOrEmpty()) {
                throw IOException("Playlist response incomplete.")
            }
            return Playlist(id, name)
        }
    }

    private fun addTracks(token: String, playlistId: String, uris: List<String> = sampleTrackUris) {
        val payload = JSONObject().apply {
            put("uris", JSONArray(uris))
        }
        val request = Request.Builder()
            .url("https://api.spotify.com/v1/playlists/$playlistId/tracks")
            .addHeader("Authorization", "Bearer $token")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Adding tracks failed: ${response.code} ${response.body?.string()}")
            }
        }
    }

    suspend fun fetchRecentTrackNames( accessToken: String, limit: Int = 20 ): List<String> = withContext(Dispatchers.IO) {
        val url = "https://api.spotify.com/v1/me/player/recently-played?limit=$limit"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext emptyList()
                }

                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val items = json.optJSONArray("items") ?: return@withContext emptyList<String>()

                val names = mutableListOf<String>()
                for (i in 0 until items.length()) {
                    val item = items.optJSONObject(i) ?: continue
                    val track = item.optJSONObject("track") ?: continue
                    val name = track.optString("name")
                    if (name.isNotBlank()) {
                        names += name
                    }
                }
                names
            }
        } catch (e: IOException) {
            emptyList()
        }
    }
}
