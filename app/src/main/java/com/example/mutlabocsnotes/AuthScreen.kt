package com.example.mutlabocsnotes

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.yandex.authsdk.YandexAuthLoginOptions
import com.yandex.authsdk.YandexAuthOptions
import com.yandex.authsdk.YandexAuthResult
import com.yandex.authsdk.YandexAuthSdk

const val AUTH_EMAIL_FIELD_TEST_TAG = "auth_email_field"
const val AUTH_PASSWORD_FIELD_TEST_TAG = "auth_password_field"
const val AUTH_SIGN_IN_BUTTON_TEST_TAG = "auth_sign_in_button"
const val AUTH_SIGN_UP_BUTTON_TEST_TAG = "auth_sign_up_button"
const val AUTH_GOOGLE_SIGN_IN_BUTTON_TEST_TAG = "auth_google_sign_in_button"
const val AUTH_YANDEX_SIGN_IN_BUTTON_TEST_TAG = "auth_yandex_sign_in_button"
const val AUTH_ERROR_MESSAGE_TEST_TAG = "auth_error_message"

@Composable
fun AuthScreen(
    uiState: AuthUiState,
    onSignIn: (email: String, password: String) -> Unit,
    onSignUp: (email: String, password: String) -> Unit,
    onGoogleIdToken: (String) -> Unit,
    onYandexAccessToken: (String) -> Unit,
    onGoogleTokenEmpty: () -> Unit,
    onGoogleSignInFailed: () -> Unit,
    onYandexTokenEmpty: () -> Unit,
    onYandexSignInFailed: () -> Unit,
    onYandexSignInCancelled: () -> Unit,
    onMessageShown: (Long) -> Unit,
    onClearInlineError: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    val googleWebClientId = stringResource(id = R.string.google_web_client_id)
    val inlineErrorMessage = uiState.inlineErrorMessage?.asString()
    val snackbarMessage = uiState.uiMessage?.text?.asString()
    val googleSignInFailedMessage = stringResource(R.string.auth_error_google_sign_in_failed)
    val yandexSignInFailedMessage = stringResource(R.string.auth_error_yandex_sign_in_failed)

    LaunchedEffect(uiState.uiMessage?.id) {
        val message = uiState.uiMessage ?: return@LaunchedEffect
        val text = snackbarMessage ?: return@LaunchedEffect
        scaffoldState.snackbarHostState.showSnackbar(text)
        onMessageShown(message.id)
    }

    val googleSignInClient = remember(context, googleWebClientId, isPreview) {
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
                    onGoogleTokenEmpty()
                }
            } else {
                Log.e("Auth", googleSignInFailedMessage, task.exception)
                onGoogleSignInFailed()
            }
        }
    }

    val yandexLauncher = if (!isPreview && yandexAuthSdk != null) {
        rememberLauncherForActivityResult(yandexAuthSdk.contract) { result ->
            when (result) {
                is YandexAuthResult.Success -> {
                    val accessToken = result.token.value.trim()
                    if (accessToken.isBlank()) {
                        onYandexTokenEmpty()
                    } else {
                        onYandexAccessToken(accessToken)
                    }
                }

                is YandexAuthResult.Failure -> {
                    Log.e("Auth", yandexSignInFailedMessage, result.exception)
                    onYandexSignInFailed()
                }

                YandexAuthResult.Cancelled -> {
                    onYandexSignInCancelled()
                }
            }
        }
    } else {
        null
    }

    Scaffold(scaffoldState = scaffoldState) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    onClearInlineError()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AUTH_EMAIL_FIELD_TEST_TAG),
                label = { Text(stringResource(R.string.auth_email_label)) },
                enabled = !uiState.isLoading
            )
            Spacer(Modifier.padding(6.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    onClearInlineError()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AUTH_PASSWORD_FIELD_TEST_TAG),
                label = { Text(stringResource(R.string.auth_password_label)) },
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    val description = if (passwordVisible) {
                        stringResource(R.string.auth_hide_password)
                    } else {
                        stringResource(R.string.auth_show_password)
                    }
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = description
                        )
                    }
                },
                enabled = !uiState.isLoading
            )
            Spacer(Modifier.padding(6.dp))
            Button(
                onClick = {
                    onClearInlineError()
                    onSignIn(email.trim(), password)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AUTH_SIGN_IN_BUTTON_TEST_TAG),
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
            if (inlineErrorMessage != null) {
                Text(
                    text = inlineErrorMessage,
                    color = MaterialTheme.colors.error,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .testTag(AUTH_ERROR_MESSAGE_TEST_TAG)
                )
            }

            Spacer(Modifier.padding(6.dp))

            Button(
                onClick = {
                    onClearInlineError()
                    onSignUp(email.trim(), password)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AUTH_SIGN_UP_BUTTON_TEST_TAG),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AUTH_GOOGLE_SIGN_IN_BUTTON_TEST_TAG),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AUTH_YANDEX_SIGN_IN_BUTTON_TEST_TAG),
                enabled = !uiState.isLoading
            ) {
                Text(stringResource(R.string.auth_sign_in_yandex))
            }
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
            uiState = AuthUiState(authState = AuthState.Unauthenticated),
            onSignIn = { _, _ -> },
            onSignUp = { _, _ -> },
            onGoogleIdToken = {},
            onYandexAccessToken = {},
            onGoogleTokenEmpty = {},
            onGoogleSignInFailed = {},
            onYandexTokenEmpty = {},
            onYandexSignInFailed = {},
            onYandexSignInCancelled = {},
            onMessageShown = {},
            onClearInlineError = {}
        )
    }
}
