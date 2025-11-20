package com.cs407.chatgplaylist.spotify

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object SpotifySearch {
    private val client = OkHttpClient()

    data class Match(val uri: String, val url: String)

    fun searchTrack(token: String, title: String, artist: String): Match? {
        if (title.isBlank()) return null
        val query = buildQuery(title, artist)
        val request = Request.Builder()
            .url("https://api.spotify.com/v1/search?type=track&limit=1&q=$query")
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Search failed: ${response.code} ${response.message}")
            }
            val body = response.body?.string().orEmpty()
            val json = JSONObject(body)
            val tracks = json.optJSONObject("tracks")
            val items = tracks?.optJSONArray("items")
            if (items == null || items.length() == 0) return null
            val first = items.getJSONObject(0)
            val uri = first.optString("uri").takeIf { it.isNotBlank() } ?: return null
            val url = first.optJSONObject("external_urls")
                ?.optString("spotify")
                ?.takeIf { it.isNotBlank() }
                ?: return null
            return Match(uri = uri, url = url)
        }
    }

    private fun buildQuery(title: String, artist: String): String {
        val base = if (artist.isBlank()) {
            "track:$title"
        } else {
            "track:$title artist:$artist"
        }
        return URLEncoder.encode(base, StandardCharsets.UTF_8.toString())
    }
}
