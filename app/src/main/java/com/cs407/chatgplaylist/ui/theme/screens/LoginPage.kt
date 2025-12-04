package com.cs407.chatgplaylist.ui.theme.screens

import android.util.Log
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.cs407.chatgplaylist.viewmodels.LoginSignupViewModel

@Composable
fun ErrorText(error: String?) {
    if (error != null) {
        Text(
            text = error,
            color = Color.Red,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LoginPage(
    modifier: Modifier = Modifier,
    onLoginComplete: (UserState) -> Unit,
    LoginSignupViewModel: LoginSignupViewModel = viewModel()
) {
    val email = LoginSignupViewModel.email
    val password = LoginSignupViewModel.password
    val error = LoginSignupViewModel.error
    val askName = LoginSignupViewModel.askName

    // Auto-login if there is already a signed-in Firebase user
    LaunchedEffect(Unit) {
        LoginSignupViewModel.tryAutoLogin(onLoginComplete)
    }

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

                TextField(
                    value = email,
                    onValueChange = { LoginSignupViewModel.onEmailChange(it) },
                    label = { Text(stringResource(R.string.email_hint)) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = password,
                    onValueChange = { LoginSignupViewModel.onPasswordChange(it) },
                    label = { Text(stringResource(R.string.password_hint)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { LoginSignupViewModel.onLoginClick(onLoginComplete) }) {
                    Text(stringResource(R.string.login_button))
                }
            } else {
                AskNamePage(
                    LoginSignupViewModel = LoginSignupViewModel,
                    onLoginComplete = onLoginComplete
                )
            }
        }
    }
}

@Composable
fun AskNamePage(
    LoginSignupViewModel: LoginSignupViewModel,
    onLoginComplete: (UserState) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    TextField(
        value = name,
        onValueChange = { name = it },
        label = { Text(stringResource(R.string.name_hint)) }
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = {
        LoginSignupViewModel.confirmName(name, onLoginComplete)
    }) {
        Text(stringResource(R.string.confirm_button))
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