package com.cs407.chatgplaylist.viewmodels

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.chatgplaylist.data.Playlist
import com.cs407.chatgplaylist.data.PlaylistDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UploadViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PlaylistDatabase.getDatabase(application)
    private val playlistDao = db.playlistDao()

    var playlists by mutableStateOf<List<Playlist>>(emptyList())
        private set

    var playlistDescription by mutableStateOf("")
        private set

    var imageUri by mutableStateOf<Uri?>(null)
        private set

    fun loadPlaylists(userId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = playlistDao.getPlaylistsWithSongs(userId).map { it.playlist }
            withContext(Dispatchers.Main) {
                playlists = list
            }
        }
    }

    fun updateDescription(newDescription: String) {
        playlistDescription = newDescription
    }

    fun updateImageUri(uri: Uri?) {
        imageUri = uri
    }

    suspend fun createTempPlaylist(userId: Int): Int {
        return withContext(Dispatchers.IO) {
            val tempPlaylist = Playlist(
                userId = userId,
                title = ""
            )
            val newId = playlistDao.insertPlaylist(tempPlaylist).toInt()

            val updatedPlaylist = tempPlaylist.copy(
                playlistId = newId,
                title = "New Playlist"
            )
            playlistDao.updatePlaylist(updatedPlaylist)

            newId
        }
    }
}