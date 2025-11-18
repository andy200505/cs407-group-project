package com.cs407.chatgplaylist.ui.theme.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cs407.chatgplaylist.R
import com.cs407.chatgplaylist.data.PlaylistDatabase
import com.cs407.chatgplaylist.data.UserState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch


@Composable
fun ProfileButtons(
    navLogOut: () -> Unit,
    userState: UserState,
    playlistDB: PlaylistDatabase,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        //logout
        Button(onClick = { navLogOut() }) {
            Text("Logout")
        }

        //del account
        Button(
            onClick = {
                scope.launch {
                    // Delete user and all playlists/songs from Room
                    playlistDB.deleteDao().deleteUserAndPlaylists(userState.id)
                    // Delete Firebase user
                    Firebase.auth.currentUser?.delete()?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            navLogOut() // navigate back to login after deletion
                        }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Delete Account", color = MaterialTheme.colorScheme.onError)
        }
    }
}


@Composable
fun ProfileScreen(
    navController: NavController,
    userState: UserState
) {
    var darkModeEnabled by remember { mutableStateOf(false) }
    val playlistDB = PlaylistDatabase.getDatabase(LocalContext.current)

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Back arrow return to upload page
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Profile pic
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(120.dp)
                        .padding(top = 32.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Username
                Text(
                    text = userState.name,
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                //Profile buttons: Logout and Delete Account
                ProfileButtons(
                    navLogOut = {
                        Firebase.auth.signOut()
                        navController.navigate("login") {
                            popUpTo("upload") { inclusive = true }
                        }
                    },
                    userState = userState,
                    playlistDB = playlistDB
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Dark mode switch (TODO: implement)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dark Mode", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = darkModeEnabled,
                        onCheckedChange = { darkModeEnabled = it }
                    )
                }
            }
        }
    }
}
