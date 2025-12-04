package com.cs407.chatgplaylist.ui.theme.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cs407.chatgplaylist.MainActivity
import com.cs407.chatgplaylist.data.Playlist
import com.cs407.chatgplaylist.data.PlaylistDatabase
import com.cs407.chatgplaylist.ui.theme.components.SongItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.cs407.chatgplaylist.viewmodels.PlaylistViewModel

@Composable
fun PlaylistScreen(navController: NavController, playlistId: Int, viewModel: PlaylistViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val isSpotifyConnected by MainActivity.spotifyConnected
    LaunchedEffect(playlistId) {
        viewModel.loadPlaylist(playlistId)
    }

    val titleText = viewModel.titleText
    val playlistSongs = viewModel.playlistSongs

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            IconButton(
                onClick = {
                    navController.popBackStack(
                        route = "upload",
                        inclusive = false
                    )
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                //title
                TextField(
                    value = titleText,
                    onValueChange = { newTitle ->
                        viewModel.updateTitle(newTitle)
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(horizontal = 16.dp),
                    textStyle = MaterialTheme.typography.titleLarge,
                    singleLine = true,
                    placeholder = { Text("Playlist Name") },
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
                            SongItem(
                                title = song.title,
                                artist = song.artist,
                                spotifyUrl = song.spotifyUrl
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { activity?.createDemoPlaylist(playlistId) },
                    enabled = isSpotifyConnected && activity != null,
                    modifier = Modifier
                        .width(200.dp)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1DB954),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Add to Spotify", style = MaterialTheme.typography.labelLarge)
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
                    Text("Save Playlist", style = MaterialTheme.typography.labelLarge)
                }

                Spacer(modifier = Modifier.height(12.dp))

                //delete playlist
                Button(
                    onClick = {
                        if (playlistId != 0) {
                            // Delete from Room database
                            viewModel.deletePlaylist(playlistId) {
                                navController.navigate("upload") {
                                    popUpTo("upload") { inclusive = true }
                                }
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
}
