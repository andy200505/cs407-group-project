package com.cs407.chatgplaylist.ui.theme.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cs407.chatgplaylist.data.PlaylistDatabase
import com.cs407.chatgplaylist.data.UserState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import com.cs407.chatgplaylist.viewmodels.ProfileViewModel


@Composable
fun ProfileButtons(
    navLogOut: () -> Unit,
    userState: UserState,
    profileViewModel: ProfileViewModel,
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

        //delete account
        Button(
            onClick = {
                profileViewModel.deleteAccount(userState.id) { success ->
                    if (success) {
                        // After successful deletion, log out + navigate as before
                        navLogOut()
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
    userState: UserState,
    darkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    profileViewModel: ProfileViewModel = viewModel()
) {

    val name = profileViewModel.displayName
    val email = profileViewModel.email
    val playlists = profileViewModel.playlistCount
    val initial = (name.trim().firstOrNull()?.uppercaseChar() ?: '?')

    LaunchedEffect(userState.id) { profileViewModel.loadProfile(userState) }

    var darkModeEnabled by remember { mutableStateOf(false) }

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
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                Spacer(modifier = Modifier.height(16.dp))

                // Profile picture
                Box(Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center) {

                    Text(initial.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)

                }

                Spacer(modifier = Modifier.height(24.dp))

                // Username
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge
                )
                // Email
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Dark mode switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dark Mode", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = darkMode,
                        onCheckedChange = { onToggleDarkMode() }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Total playlists saved: $playlists",
                    style = MaterialTheme.typography.headlineMedium
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
                    profileViewModel = profileViewModel
                )
            }
        }
    }
}
