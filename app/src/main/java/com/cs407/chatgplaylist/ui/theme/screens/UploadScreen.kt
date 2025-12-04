package com.cs407.chatgplaylist.ui.theme.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cs407.chatgplaylist.MainActivity
import com.cs407.chatgplaylist.R
import com.cs407.chatgplaylist.data.Playlist
import com.cs407.chatgplaylist.data.PlaylistDatabase
import com.cs407.chatgplaylist.data.UserState
import kotlinx.coroutines.launch
import java.io.File
import com.cs407.chatgplaylist.viewmodels.UploadViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(navController: NavController, userState: UserState, uploadViewModel: UploadViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val isSpotifyConnected by MainActivity.spotifyConnected
    // Load playlists belonging to this Room userId
    val playlists = uploadViewModel.playlists
    val playlistDescription = uploadViewModel.playlistDescription
    val imageUri = uploadViewModel.imageUri
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    fun createImageUri(context: Context): Uri {
        val imageFile = File.createTempFile("playlist_", ".jpg", context.cacheDir)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            uploadViewModel.updateImageUri(cameraUri)
        }
    }

    LaunchedEffect(userState.id) {
        if (userState.id != 0) {
            uploadViewModel.loadPlaylists(userState.id)
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
                            )
                            .clickable {
                                val uri = createImageUri(context)
                                cameraUri = uri
                                cameraLauncher.launch(uri)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (imageUri == null)
                                "Upload Your Playlist Image"
                            else
                                "Image captured"
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    TextField(
                        value = playlistDescription,
                        onValueChange = { uploadViewModel.updateDescription(it) },
                        placeholder = { Text("Describe your playlist") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        singleLine = false,
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                navController.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("prompt", uploadViewModel.playlistDescription)
                                navController.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("imageUri", uploadViewModel.imageUri?.toString())
                                val tempPlaylist = Playlist(
                                    playlistId = 0,
                                    userId = userState.id,
                                    title = ""
                                )
                                val newId = uploadViewModel.createTempPlaylist(userState.id)
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
