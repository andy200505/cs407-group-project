package com.cs407.chatgplaylist.ui.theme.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cs407.chatgplaylist.R
import com.cs407.chatgplaylist.data.PlaylistDatabase
import com.cs407.chatgplaylist.data.User
import com.cs407.chatgplaylist.data.UserState
import kotlinx.coroutines.runBlocking
//import java.security.MessageDigest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import com.google.firebase.auth.userProfileChangeRequest

@Composable
fun ErrorText(error: String?) {
    if (error != null)
        Text(text = error, color = Color.Red, textAlign = TextAlign.Center)
}

@Composable
fun userEmail(): String {
    var email by remember { mutableStateOf("") }

    TextField(
        value = email,
        onValueChange = { email = it },
        label = { Text(stringResource(R.string.email_hint)) })

    return email
}

@Composable
fun userPassword(): String {
    var passwd by remember { mutableStateOf("") }

    TextField(
        value = passwd,
        onValueChange = { passwd = it },
        label = { Text(stringResource(R.string.password_hint)) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
    )

    return passwd
}

fun createAccount(
    email: String,
    password: String,
    onComplete: (Boolean, Exception?, FirebaseUser?) -> Unit,
) {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            onComplete(task.isSuccessful, task.exception, auth.currentUser)
        }
}

fun signIn(
    email: String,
    password: String,
    onComplete: (Boolean, Exception?, FirebaseUser?) -> Unit,
) {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful)
                onComplete(task.isSuccessful, task.exception, auth.currentUser)
            else
                createAccount(email, password, onComplete)
        }
}

//fun hash(input: String): String {
//    return MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
//        .fold("") { str, it -> str + "%02x".format(it) }
//}

@Composable
fun LogInSignUpButton(
    email: String,
    password: String,
    onComplete: (Boolean, Exception?, FirebaseUser?) -> Unit
) {
    val context = LocalContext.current
//    val userPasswdKV =
//        context.getSharedPreferences(context.getString(R.string.userPasswdKV), Context.MODE_PRIVATE)

    Button(onClick = {
        var errorString: String? = null

        val emailResult = checkEmail(email)
        if (emailResult == EmailResult.Empty) {
            errorString = context.getString(R.string.empty_email)
        } else if (emailResult == EmailResult.Invalid) {
            errorString = context.getString(R.string.invalid_email)
        }

        val passwordResult = checkPassword(password)
        if (errorString == null) {
            errorString = when (passwordResult) {
                PasswordResult.Empty -> {
                    context.getString(R.string.empty_password)
                }

                PasswordResult.Short -> {
                    context.getString(R.string.short_password)
                }

                PasswordResult.Invalid -> {
                    context.getString(R.string.invalid_password)
                }

                PasswordResult.Valid -> {
                    null
                }
            }
        }

        if (errorString != null)
            onComplete(false, Exception(errorString), null)
        else
            signIn(email, password, onComplete)
    }) {
        Text(stringResource(R.string.login_button))
    }
}

enum class EmailResult {
    Valid,
    Empty,
    Invalid,
}

fun checkEmail(email: String): EmailResult {
    if (email.isEmpty())
        return EmailResult.Empty
    // 1. username of email should only contain "0-9, a-z, _, A-Z, ."
    // 2. there is one and only one "@" between username and server address
    // 3. there are multiple domain names with at least one top-level domain
    // 4. domain name "0-9, a-z, -, A-Z" (could not have "_" but "-" is valid)
    // 5. multiple domain separate with '.'
    // 6. top level domain should only contain letters and at lest 2 letters
    val pattern = Regex("^[\\w.]+@([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$")
    return if (pattern.matches(email)) EmailResult.Valid else EmailResult.Invalid
}

enum class PasswordResult {
    Valid,
    Empty,
    Short,
    Invalid
}

fun checkPassword(password: String): PasswordResult {
    // 1. password should contain at least one uppercase letter, lowercase letter, one digit
    // 2. minimum length: 5
    if (password.isEmpty())
        return PasswordResult.Empty
    if (password.length < 5)
        return PasswordResult.Short
    if (Regex("\\d+").containsMatchIn(password) &&
        Regex("[a-z]+").containsMatchIn(password) &&
        Regex("[A-Z]+").containsMatchIn(password)
    )
        return PasswordResult.Valid
    return PasswordResult.Invalid
}

fun updateName(name: String, onComplete: (Boolean, Exception?, FirebaseUser?) -> Unit) {
    val user = Firebase.auth.currentUser

    val profileUpdates = userProfileChangeRequest {
        displayName = name
//        photoUri = Uri.parse("https://example.com/jane-q-user/profile.jpg")
    }

    user!!.updateProfile(profileUpdates)
        .addOnCompleteListener { task ->
            onComplete(task.isSuccessful, task.exception, user)
        }
}

@Composable
fun AskNamePage(
    onComplete: (Boolean, Exception?, FirebaseUser?) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    TextField(
        value = name,
        onValueChange = { name = it },
        label = { Text(stringResource(R.string.name_hint)) })
    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = {
        updateName(name, onComplete)
    }) {
        Text(stringResource(R.string.confirm_button))
    }
}

@Composable
fun LoginPage(
    modifier: Modifier = Modifier,
    onLoginComplete: (UserState) -> Unit
) {
    var email: String
    var password: String
    var error: String? by remember { mutableStateOf(null) }
    var askName: Boolean by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val playlistDB = PlaylistDatabase.getDatabase(context)

    val onComplete: (Boolean, Exception?, FirebaseUser?) -> Unit =
        { isSuccess, taskException, signedUser ->
            if (isSuccess && signedUser != null)
                if (signedUser.displayName != null && !signedUser.displayName!!.isEmpty()) {
                    var user: User?
                    runBlocking {
                        user = playlistDB.userDao().getByUID(signedUser.uid)
                        if (user == null) {
                            playlistDB.userDao().insert(User(userUID = signedUser.uid))
                            user = playlistDB.userDao().getByUID(signedUser.uid)
                        }
                    }
                    onLoginComplete(UserState(user!!.userId, signedUser.displayName!!,
                        signedUser.uid
                    ))
                } else {
                    askName = true
                }
            else
                error = taskException?.message
        }

    val user = Firebase.auth.currentUser
    if (user != null) onComplete(true, null, user)

    Scaffold(modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!askName) {
                ErrorText(error)
                email = userEmail()
                password = userPassword()
                Spacer(modifier = Modifier.height(16.dp))
                LogInSignUpButton(email, password, onComplete)
            } else {
                AskNamePage(onComplete)
            }
        }
    }
}