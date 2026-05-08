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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.yandex.authsdk.YandexAuthLoginOptions
import com.yandex.authsdk.YandexAuthOptions
import com.yandex.authsdk.YandexAuthResult
import com.yandex.authsdk.YandexAuthSdk

@Composable
fun AuthScreen(
    uiState: AuthUiState,
    onSignIn: (email: String, password: String) -> Unit,
    onSignUp: (email: String, password: String) -> Unit,
    onGoogleIdToken: (String) -> Unit,
    onYandexAccessToken: (String) -> Unit,
    onClearError: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    val googleWebClientId = stringResource(id = R.string.google_web_client_id)
    val errorMessage = uiState.errorMessage?.asString()
    val googleTokenEmptyMessage = stringResource(R.string.auth_error_google_token_empty)
    val googleSignInFailedMessage = stringResource(R.string.auth_error_google_sign_in_failed)
    val yandexTokenEmptyMessage = stringResource(R.string.auth_error_yandex_token_empty)
    val yandexSignInFailedMessage = stringResource(R.string.auth_error_yandex_sign_in_failed)
    val yandexSignInCancelledMessage = stringResource(R.string.auth_error_yandex_sign_in_cancelled)

    LaunchedEffect(uiState.errorMessage) {
        val message = errorMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, LENGTH_SHORT).show()
        onClearError()
    }

    val googleSignInClient = remember {
        if (isPreview) {
            null
        } else {
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
            YandexAuthSdk.create(YandexAuthOptions(context, true))
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
                    onGoogleIdToken(idToken)
                } else {
                    Toast.makeText(context, googleTokenEmptyMessage, LENGTH_SHORT).show()
                }
            } else {
                Log.e("Auth", googleSignInFailedMessage, task.exception)
                Toast.makeText(
                    context,
                    task.exception?.localizedMessage ?: googleSignInFailedMessage,
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
                        Toast.makeText(context, yandexTokenEmptyMessage, LENGTH_SHORT).show()
                    } else {
                        onYandexAccessToken(accessToken)
                    }
                }

                is YandexAuthResult.Failure -> {
                    Log.e("Auth", yandexSignInFailedMessage, result.exception)
                    Toast.makeText(
                        context,
                        result.exception.localizedMessage ?: yandexSignInFailedMessage,
                        LENGTH_SHORT
                    ).show()
                }

                YandexAuthResult.Cancelled -> {
                    Toast.makeText(context, yandexSignInCancelledMessage, LENGTH_SHORT).show()
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
            label = { Text(stringResource(R.string.auth_email_label)) },
            enabled = !uiState.isLoading
        )
        Spacer(Modifier.padding(6.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.auth_password_label)) },
            enabled = !uiState.isLoading
        )
        Spacer(Modifier.padding(6.dp))
        Button(
            onClick = { onSignIn(email.trim(), password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading
        ) {
            Text(
                if (uiState.isLoading) {
                    stringResource(R.string.auth_signing_in)
                } else {
                    stringResource(R.string.auth_sign_in)
                }
            )
        }

        Spacer(Modifier.padding(6.dp))

        Button(
            onClick = { onSignUp(email.trim(), password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading
        ) {
            Text(stringResource(R.string.auth_sign_up))
        }

        Spacer(Modifier.padding(6.dp))

        Button(
            onClick = {
                if (!isPreview) {
                    googleSignInClient?.signInIntent?.let(googleLauncher::launch)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading
        ) {
            Text(stringResource(R.string.auth_sign_in_google))
        }

        Spacer(Modifier.padding(6.dp))

        Button(
            onClick = {
                if (!isPreview) {
                    yandexLauncher?.launch(YandexAuthLoginOptions())
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading
        ) {
            Text(stringResource(R.string.auth_sign_in_yandex))
        }
    }
}

@Preview(
    name = "Auth screen (light)",
    showBackground = true,
    backgroundColor = 0xFFFFFF
)
@Composable
fun AuthScreenPreview() {
    MaterialTheme {
        AuthScreen(
            uiState = AuthUiState(authState = AuthState.Unauthenticated()),
            onSignIn = { _, _ -> },
            onSignUp = { _, _ -> },
            onGoogleIdToken = {},
            onYandexAccessToken = {},
            onClearError = {}
        )
    }
}
