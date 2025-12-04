package com.cs407.chatgplaylist.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.chatgplaylist.R
import com.cs407.chatgplaylist.data.PlaylistDatabase
import com.cs407.chatgplaylist.data.User
import com.cs407.chatgplaylist.data.UserState
import com.cs407.chatgplaylist.ui.theme.screens.EmailResult
import com.cs407.chatgplaylist.ui.theme.screens.PasswordResult
import com.cs407.chatgplaylist.ui.theme.screens.checkEmail
import com.cs407.chatgplaylist.ui.theme.screens.checkPassword
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginSignupViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    private val db = PlaylistDatabase.getDatabase(application)
    private val userDao = db.userDao()

    var email by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var askName by mutableStateOf(false)
        internal set

    fun onEmailChange(newEmail: String) {
        email = newEmail
    }

    fun onPasswordChange(newPassword: String) {
        password = newPassword
    }

    /**
     * Attempt auto-login with an already signed-in Firebase user.
     */
    fun tryAutoLogin(onLoginComplete: (UserState) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            handleFirebaseUserSuccess(currentUser, onLoginComplete)
        }
    }

    /**
     * Called when the user taps the Login/Sign Up button.
     * Performs validation, then sign-in or sign-up with Firebase.
     */
    fun onLoginClick(onLoginComplete: (UserState) -> Unit) {
        error = null

        val context = getApplication<Application>()
        val emailResult = checkEmail(email)
        val passwordResult = checkPassword(password)

        val errorString = when {
            emailResult == EmailResult.Empty ->
                context.getString(R.string.empty_email)

            emailResult == EmailResult.Invalid ->
                context.getString(R.string.invalid_email)

            passwordResult == PasswordResult.Empty ->
                context.getString(R.string.empty_password)

            passwordResult == PasswordResult.Short ->
                context.getString(R.string.short_password)

            passwordResult == PasswordResult.Invalid ->
                context.getString(R.string.invalid_password)

            else -> null
        }

        if (errorString != null) {
            error = errorString
            return
        }

        // Try sign-in first
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful && auth.currentUser != null) {
                    handleFirebaseUserSuccess(auth.currentUser!!, onLoginComplete)
                } else {
                    // If sign-in fails, try creating an account
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { createTask ->
                            if (createTask.isSuccessful && auth.currentUser != null) {
                                handleFirebaseUserSuccess(auth.currentUser!!, onLoginComplete)
                            } else {
                                error = createTask.exception?.message ?: task.exception?.message
                            }
                        }
                }
            }
    }

    /**
     * Called from AskNamePage when the user confirms their name.
     * Updates the Firebase profile first, then runs the usual success flow.
     */
    fun confirmName(name: String, onLoginComplete: (UserState) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            error = "No logged in user."
            return
        }

        val profileUpdates = userProfileChangeRequest {
            displayName = name
        }

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    handleFirebaseUserSuccess(user, onLoginComplete)
                } else {
                    error = task.exception?.message
                }
            }
    }

    /**
     * Common handler for successful Firebase sign-in / sign-up.
     * Ensures the user exists in Room and then triggers onLoginComplete.
     */
    private fun handleFirebaseUserSuccess(
        firebaseUser: FirebaseUser,
        onLoginComplete: (UserState) -> Unit
    ) {
        val displayName = firebaseUser.displayName
        if (displayName.isNullOrBlank()) {
            // Ask for name on the UI
            askName = true
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            var user = userDao.getByUID(firebaseUser.uid)
            if (user == null) {
                userDao.insert(User(userUID = firebaseUser.uid))
                user = userDao.getByUID(firebaseUser.uid)
            }

            val finalUser = user!!
            val userState = UserState(
                id = finalUser.userId,
                name = displayName,
                uid = firebaseUser.uid
            )

            withContext(Dispatchers.Main) {
                onLoginComplete(userState)
            }
        }
    }
}