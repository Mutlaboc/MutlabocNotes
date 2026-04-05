package com.example.mutlabocsnotes

import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mutlabocsnotes.auth.BackendAuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.yandex.authsdk.YandexAuthLoginOptions
import com.yandex.authsdk.YandexAuthOptions
import com.yandex.authsdk.YandexAuthResult
import com.yandex.authsdk.YandexAuthSdk
import kotlinx.coroutines.launch

// Composable that renders auth screen.
@Composable
fun AuthScreen(onAuthenticated: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    val googleWebClientId = stringResource(id = R.string.google_web_client_id)
    val backendAuthRepository = remember(context) { BackendAuthRepository(context) }
    val scope = rememberCoroutineScope()

    val googleSignInClient = remember {
        if (isPreview) null else {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(googleWebClientId)
                .requestEmail()
                .build()
            GoogleSignIn.getClient(context, gso)
        }
    }

    val yandexAuthSdk = remember(context, isPreview) {
        if (isPreview) {
            null
        } else {
            YandexAuthSdk.create(
                YandexAuthOptions(
                    context,
                    true
                )
            )
        }
    }

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (!isPreview) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            if (task.isSuccessful) {
                val idToken = task.result.idToken
                if (!idToken.isNullOrBlank()) {
                    scope.launch {
                        runCatching {
                            backendAuthRepository.signInWithGoogle(idToken)
                        }.onSuccess {
                            onAuthenticated()
                        }.onFailure { error ->
                            Log.e("Auth", "Backend Google sign-in failed", error)
                            Toast.makeText(
                                context,
                                error.localizedMessage ?: "Ошибка входа через backend",
                                LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Google idToken is empty", LENGTH_SHORT).show()
                }
            } else {
                Log.e("Auth", "Google sign-in failed", task.exception)
                Toast.makeText(
                    context,
                    task.exception?.localizedMessage ?: "Ошибка входа через Google",
                    LENGTH_SHORT
                ).show()
            }
        }
    }

    val yandexLauncher = if (!isPreview && yandexAuthSdk != null) {
        rememberLauncherForActivityResult(yandexAuthSdk.contract) { result ->
            when (result) {
                is YandexAuthResult.Success -> {
                    val accessToken = result.token.value.trim()
                    if (accessToken.isBlank()) {
                        Toast.makeText(context, "Yandex access token is empty", LENGTH_SHORT).show()
                        return@rememberLauncherForActivityResult
                    }

                    scope.launch {
                        runCatching {
                            backendAuthRepository.signInWithYandex(accessToken)
                        }.onSuccess {
                            onAuthenticated()
                        }.onFailure { error ->
                            Log.e("Auth", "Backend Yandex sign-in failed", error)
                            Toast.makeText(
                                context,
                                error.localizedMessage ?: "Ошибка входа через Яндекс",
                                LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                is YandexAuthResult.Failure -> {
                    Log.e("Auth", "Yandex SDK sign-in failed", result.exception)
                    Toast.makeText(
                        context,
                        result.exception.localizedMessage ?: "Ошибка входа через Яндекс",
                        LENGTH_SHORT
                    ).show()
                }

                YandexAuthResult.Cancelled -> {
                    Toast.makeText(context, "Вход через Яндекс отменён", LENGTH_SHORT).show()
                }
            }
        }
    } else {
        null
    }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") }
        )
        Spacer(Modifier.padding(6.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") }
        )
        Spacer(Modifier.padding(6.dp))
        Button(
            onClick = {
                if (email.isBlank() || password.length < 6) {
                    Toast.makeText(
                        context,
                        "Введите корректный e-mail и пароль больше 6 символов",
                        LENGTH_SHORT
                    ).show()
                    return@Button
                }

                if (!isPreview) {
                    scope.launch {
                        runCatching {
                            backendAuthRepository.signInWithEmail(
                                email = email.trim(),
                                password = password
                            )
                        }.onSuccess {
                            onAuthenticated()
                        }.onFailure { error ->
                            Log.e("Auth", "Backend email sign-in failed", error)
                            Toast.makeText(
                                context,
                                error.localizedMessage ?: "Ошибка входа",
                                LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign In")
        }

        Spacer(Modifier.padding(6.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.length < 6) {
                    Toast.makeText(
                        context,
                        "Введите корректный e-mail и больше 6 символов",
                        LENGTH_SHORT
                    ).show()
                    return@Button
                }

                if (!isPreview) {
                    scope.launch {
                        runCatching {
                            backendAuthRepository.signUpWithEmail(
                                email = email.trim(),
                                password = password
                            )
                        }.onSuccess {
                            onAuthenticated()
                        }.onFailure { error ->
                            Log.e("Auth", "Backend email sign-up failed", error)
                            Toast.makeText(
                                context,
                                error.localizedMessage ?: "Ошибка регистрации",
                                LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Up")
        }

        Spacer(Modifier.padding(6.dp))

        Button(
            onClick = {
                if (!isPreview) {
                    googleLauncher.launch(googleSignInClient?.signInIntent)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign in with Google")
        }

        Spacer(Modifier.padding(6.dp))

        Button(
            onClick = {
                if (!isPreview) {
                    yandexLauncher?.launch(YandexAuthLoginOptions())
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign in with Yandex")
        }
    }
}

@Preview(
    name = "Auth screen (light)",
    showBackground = true,
    backgroundColor = 0xFFFFFF
)
// Preview composable for design-time inspection in Android Studio.
@Composable
fun AuthScreenPreview() {
    MaterialTheme {
        AuthScreen(onAuthenticated = {})
    }
}
