package com.cs407.chatgplaylist.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.chatgplaylist.data.PlaylistDatabase
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PlaylistDatabase.getDatabase(application)
    private val deleteDao = db.deleteDao()

    var isDeleting by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun deleteAccount(
        userId: Int,
        onCompleted: (Boolean) -> Unit
    ) {
        isDeleting = true
        errorMessage = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                deleteDao.deleteUserAndPlaylists(userId)

                withContext(Dispatchers.Main) {
                    val firebaseUser = Firebase.auth.currentUser
                    if (firebaseUser == null) {
                        isDeleting = false
                        onCompleted(true)
                    } else {
                        firebaseUser.delete().addOnCompleteListener { task ->
                            isDeleting = false
                            if (!task.isSuccessful) {
                                errorMessage = task.exception?.message
                            }
                            onCompleted(task.isSuccessful)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isDeleting = false
                    errorMessage = e.message
                    onCompleted(false)
                }
            }
        }
    }
}