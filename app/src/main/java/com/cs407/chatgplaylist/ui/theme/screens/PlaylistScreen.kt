package com.cs407.chatgplaylist.ui.theme.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cs407.chatgplaylist.R
import com.cs407.chatgplaylist.data.DeleteDao
import com.cs407.chatgplaylist.data.Playlist
import com.cs407.chatgplaylist.data.PlaylistDatabase
import com.cs407.chatgplaylist.ui.theme.components.SongItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color

@Composable
fun PlaylistScreen(navController: NavController, playlistId: Int) {
    val context = LocalContext.current
    val db = remember { PlaylistDatabase.getDatabase(context) }
    val playlistDao = db.playlistDao()
    val deleteDao = db.deleteDao()
    val scope = rememberCoroutineScope()

    var playlist by remember { mutableStateOf<Playlist?>(null) }
    var titleText by remember { mutableStateOf("") }
    var playlistSongs by remember { mutableStateOf<List<com.cs407.chatgplaylist.data.Song>>(emptyList()) }

    LaunchedEffect(playlistId) {
        playlist = playlistDao.getPlaylistById(playlistId)
        titleText = playlist?.title ?: ""

        playlist?.let {
            //load songs that were added by the backend AI
            playlistSongs = playlistDao.getSongsForPlaylist(it.playlistId)
        }
    }

    //demo songs
    //val songs = List(20) { "Song Title ${it + 1}" to "Artist ${it + 1}" }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            /*
            Text(
                text = "Success! Here's your playlist",
                style = MaterialTheme.typography.headlineMedium
            )
             */

            Spacer(modifier = Modifier.height(20.dp))

            //title
            TextField(
                value = titleText,
                onValueChange = { newTitle ->
                    titleText = newTitle
                    playlist?.let {
                        scope.launch(Dispatchers.IO) {
                            playlistDao.updatePlaylist(it.copy(title = newTitle))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(horizontal = 16.dp),
                textStyle = MaterialTheme.typography.titleLarge,
                singleLine = true,
                placeholder = { Text("Playlist Name") }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(playlistSongs) { song ->
                        SongItem(title = song.title, artist = song.artist)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { /* TODO: integrate with Spotify */ },
                modifier = Modifier
                    .width(200.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1DB954),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "Add to Spotify", style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(modifier = Modifier.height(20.dp))

            //save playlist
            Button(
                onClick = {
                    navController.popBackStack(route = "upload", inclusive = false)
                },
                modifier = Modifier
                    .width(200.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Playlist")
            }

            Spacer(modifier = Modifier.height(12.dp))

            //delete playlist
            Button(
                onClick = {
                    if (playlistId != 0) {
                        // Delete from Room database
                        CoroutineScope(Dispatchers.IO).launch {
                            deleteDao.deletePlaylistAndSongs(playlistId)
                        }
                    }
                    navController.navigate("upload") {
                        popUpTo("upload") { inclusive = true }
                    }
                },
                modifier = Modifier
                    .width(200.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Delete Playlist")
            }
        }
    }
}