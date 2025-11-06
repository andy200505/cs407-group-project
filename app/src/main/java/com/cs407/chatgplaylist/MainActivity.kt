package com.cs407.chatgplaylist

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cs407.chatgplaylist.auth.SpotifyAuth
import com.cs407.chatgplaylist.ui.theme.ChatGPlaylisTTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AppState {
    val displayName = mutableStateOf<String?>(null)
}

class MainActivity : ComponentActivity() {

    companion object {
        var signedInDisplayName: String? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChatGPlaylisTTheme {
                AppNavigation()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (SpotifyAuth.isSpotifyRedirect(intent)) {
            Toast.makeText(this, "Returned from Spotify auth", Toast.LENGTH_SHORT).show()
        }
    }

    fun startSpotifyAuth() {
        val clientId = getString(R.string.spotify_client_id)
        val redirect = getString(R.string.spotify_redirect_uri)
        SpotifyAuth.startSignIn(this, clientId, redirect)
    }

}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "upload") {
        composable("upload") { UploadScreen(navController) }
        composable("playlist") { PlaylistScreen(navController) }
    }
}

@Composable
fun UploadScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as? MainActivity

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "ChatGPlaylisT",
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

            Button(
                onClick = { navController.navigate("playlist") }
            ) {
                Text("Submit")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { activity?.startSpotifyAuth() },
            ) {
                Text("Connect Spotify")
            }
        }
    }
}

@Composable
fun PlaylistScreen(navController: NavController) {
    val songs = List(20) { "Song Title ${it + 1}" to "Artist ${it + 1}" }
    val context = LocalContext.current
    val activity = context as? MainActivity
    val name = AppState.displayName.value

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Success! Here's your playlist",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Gymrat Playlist",
                style = MaterialTheme.typography.titleLarge
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
                    items(songs) { (title, artist) ->
                        SongItem(title = title, artist = artist)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFF1DB954),
                    contentColor = androidx.compose.ui.graphics.Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "Add to Spotify", style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Underlined, clickable text to go back to Upload page
            Text(
                text = "Make another playlist",
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = TextDecoration.Underline
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    // Go back to the first page (upload screen)
                    navController.popBackStack(route = "upload", inclusive = false)
                }
            )
        }
    }
}

@Composable
fun SongItem(title: String, artist: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween // pushes title/artist left, icon right
    ) {
        // Left side: Song info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Right side: Spotify icon
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.spotify),
            contentDescription = "Spotify Logo",
            modifier = Modifier
                .size(24.dp)
                .padding(start = 8.dp)
        )
    }

    Divider(modifier = Modifier.padding(top = 8.dp))
}
