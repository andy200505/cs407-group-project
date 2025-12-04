package com.cs407.chatgplaylist.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.*
import com.cs407.chatgplaylist.data.Playlist
import com.cs407.chatgplaylist.data.PlaylistDatabase
import com.cs407.chatgplaylist.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PlaylistDatabase.getDatabase(application)
    private val playlistDao = db.playlistDao()
    private val deleteDao = db.deleteDao()

    var playlist by mutableStateOf<Playlist?>(null)
        private set

    var titleText by mutableStateOf("")
        private set

    var playlistSongs by mutableStateOf<List<Song>>(emptyList())
        private set

    fun loadPlaylist(playlistId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val p = playlistDao.getPlaylistById(playlistId)
            val songs = p?.let { playlistDao.getSongsForPlaylist(it.playlistId) } ?: emptyList()

            withContext(Dispatchers.Main) {
                playlist = p
                titleText = p?.title ?: ""
                playlistSongs = songs
            }
        }
    }

    fun updateTitle(newTitle: String) {
        titleText = newTitle
        val p = playlist ?: return
        viewModelScope.launch(Dispatchers.IO) {
            playlistDao.updatePlaylist(p.copy(title = newTitle))
        }
    }

    fun deletePlaylist(playlistId: Int, onDeleted: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            deleteDao.deletePlaylistAndSongs(playlistId)
            withContext(Dispatchers.Main) { onDeleted() }
        }
    }
}