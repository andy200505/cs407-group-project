package com.cs407.chatgplaylist.ui.theme.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cs407.chatgplaylist.MainActivity
import com.cs407.chatgplaylist.R
import com.cs407.chatgplaylist.data.Playlist
import com.cs407.chatgplaylist.data.PlaylistDatabase
import com.cs407.chatgplaylist.data.UserState
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(navController: NavController, userState: UserState) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val isSpotifyConnected by MainActivity.spotifyConnected
    val db = remember { PlaylistDatabase.getDatabase(context) }
    val playlistDao = db.playlistDao()
    // Load playlists belonging to this Room userId
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var playlistDescription by remember { mutableStateOf("") }
    LaunchedEffect(userState.id) {
        if (userState.id != 0) {
            playlists = playlistDao.getPlaylistsWithSongs(userState.id)
                .map { it.playlist }  // extract Playlist
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Your Playlists",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )

                playlists.forEach { playlist ->
                    PlaylistDrawerCard(title = playlist.title) {
                        scope.launch { drawerState.close() }
                        navController.navigate("playlist/${playlist.playlistId}")
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate("profile") }) {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = "Profile",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .border(
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Upload Your Playlist Image")
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    TextField(
                        value = playlistDescription,
                        onValueChange = { playlistDescription = it },
                        placeholder = { Text("Describe your playlist") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = false,
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                navController.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("prompt", playlistDescription)
                                val tempPlaylist = Playlist(
                                    playlistId = 0,
                                    userId = userState.id,
                                    title = ""
                                )
                                val newId = playlistDao.insertPlaylist(tempPlaylist).toInt()

                                val updatedPlaylist = tempPlaylist.copy(
                                    playlistId = newId,
                                    title = "New Playlist #$newId"
                                )
                                playlistDao.updatePlaylist(updatedPlaylist)

                                navController.navigate("loading/$newId")
                            }
                        },
                        modifier = Modifier
                            .width(200.dp)
                            .height(48.dp)
                    ) {
                        Text("Submit")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { activity?.startSpotifyAuth() },
                        enabled = activity != null,
                        modifier = Modifier
                            .width(200.dp)
                            .height(48.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isSpotifyConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Text(
                            text = if (isSpotifyConnected) "Spotify Connected" else "Connect Spotify"
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun PlaylistDrawerCard(
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
