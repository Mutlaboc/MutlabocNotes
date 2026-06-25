package com.example.mutlabocsnotes

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.yandex.authsdk.YandexAuthLoginOptions
import com.yandex.authsdk.YandexAuthOptions
import com.yandex.authsdk.YandexAuthResult
import com.yandex.authsdk.YandexAuthSdk

const val AUTH_EMAIL_FIELD_TEST_TAG = "auth_email_field"
const val AUTH_PASSWORD_FIELD_TEST_TAG = "auth_password_field"
const val AUTH_CONFIRM_PASSWORD_FIELD_TEST_TAG = "auth_confirm_password_field"
const val AUTH_SIGN_IN_BUTTON_TEST_TAG = "auth_sign_in_button"
const val AUTH_SIGN_UP_BUTTON_TEST_TAG = "auth_sign_up_button"
const val AUTH_GOOGLE_SIGN_IN_BUTTON_TEST_TAG = "auth_google_sign_in_button"
const val AUTH_YANDEX_SIGN_IN_BUTTON_TEST_TAG = "auth_yandex_sign_in_button"
const val AUTH_ERROR_MESSAGE_TEST_TAG = "auth_error_message"

private enum class AuthMode {
    SignIn,
    SignUp
}

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
    var authMode by remember { mutableStateOf(AuthMode.SignIn) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showPasswordMismatchError by remember { mutableStateOf(false) }

    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    val googleWebClientId = stringResource(id = R.string.google_web_client_id)
    val localErrorMessage = if (showPasswordMismatchError) {
        stringResource(R.string.auth_error_passwords_do_not_match)
    } else {
        null
    }
    val inlineErrorMessage = localErrorMessage ?: uiState.inlineErrorMessage?.asString()
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

    Scaffold(
        scaffoldState = scaffoldState,
        backgroundColor = CozyAuth.Cream
    ) { paddingValues ->
        CozyAuthScene(
            paddingValues = paddingValues,
            authMode = authMode,
            onAuthModeChange = { mode ->
                authMode = mode
                confirmPassword = ""
                showPasswordMismatchError = false
                onClearInlineError()
            },
            email = email,
            onEmailChange = {
                email = it
                showPasswordMismatchError = false
                onClearInlineError()
            },
            password = password,
            onPasswordChange = {
                password = it
                showPasswordMismatchError = false
                onClearInlineError()
            },
            confirmPassword = confirmPassword,
            onConfirmPasswordChange = {
                confirmPassword = it
                showPasswordMismatchError = false
                onClearInlineError()
            },
            passwordVisible = passwordVisible,
            onPasswordVisibilityChange = { passwordVisible = !passwordVisible },
            inlineErrorMessage = inlineErrorMessage,
            isLoading = uiState.isLoading,
            onSignUp = {
                onClearInlineError()
                if (password != confirmPassword) {
                    showPasswordMismatchError = true
                    return@CozyAuthScene
                }
                onSignUp(email.trim(), password)
            },
            onSignIn = {
                onClearInlineError()
                onSignIn(email.trim(), password)
            },
            onGoogleSignIn = {
                if (!isPreview) {
                    googleSignInClient?.signInIntent?.let(googleLauncher::launch)
                }
            },
            onYandexSignIn = {
                if (!isPreview) {
                    yandexLauncher?.launch(YandexAuthLoginOptions())
                }
            }
        )
    }
}

@Composable
private fun CozyAuthScene(
    paddingValues: PaddingValues,
    authMode: AuthMode,
    onAuthModeChange: (AuthMode) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: () -> Unit,
    inlineErrorMessage: String?,
    isLoading: Boolean,
    onSignUp: () -> Unit,
    onSignIn: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onYandexSignIn: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(CozyAuth.Cream)
            .pixelScreenFrame()
            .padding(paddingValues)
    ) {
        val compactHeight = maxHeight < 720.dp
        val horizontalPadding = if (maxWidth < 380.dp) 16.dp else 24.dp
        val topPadding = if (compactHeight) 10.dp else 34.dp
        val sceneHeight = if (compactHeight) 96.dp else 154.dp
        val cardPadding = if (compactHeight) 14.dp else 22.dp

        PixelSkyDecor(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compactHeight) 330.dp else 430.dp)
        )

        AuthBottomScene(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(sceneHeight)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPadding)
                .padding(top = topPadding, bottom = sceneHeight + 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AuthWelcomeHeader(compact = compactHeight)
            Spacer(Modifier.height(if (compactHeight) 8.dp else 24.dp))
            AuthRegistrationCard(
                authMode = authMode,
                onAuthModeChange = onAuthModeChange,
                email = email,
                onEmailChange = onEmailChange,
                password = password,
                onPasswordChange = onPasswordChange,
                confirmPassword = confirmPassword,
                onConfirmPasswordChange = onConfirmPasswordChange,
                passwordVisible = passwordVisible,
                onPasswordVisibilityChange = onPasswordVisibilityChange,
                inlineErrorMessage = inlineErrorMessage,
                isLoading = isLoading,
                onSignUp = onSignUp,
                onSignIn = onSignIn,
                onGoogleSignIn = onGoogleSignIn,
                onYandexSignIn = onYandexSignIn,
                contentPadding = cardPadding,
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AuthWelcomeHeader(compact: Boolean) {
    val titleSize = if (compact) 25.sp else 32.sp
    val brandSize = if (compact) 18.sp else 20.sp
    val subtitleSize = if (compact) 13.sp else 15.sp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.auth_welcome_title),
            color = CozyAuth.Ink,
            fontFamily = CozyAuth.PixelFont,
            fontSize = titleSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.auth_brand_name),
            color = CozyAuth.TerracottaDark,
            fontFamily = CozyAuth.PixelFont,
            fontSize = brandSize,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = stringResource(R.string.auth_subtitle),
            color = CozyAuth.InkSoft,
            fontFamily = CozyAuth.PixelFont,
            fontSize = subtitleSize,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 5.dp)
        )
    }
}

@Composable
private fun AuthRegistrationCard(
    authMode: AuthMode,
    onAuthModeChange: (AuthMode) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: () -> Unit,
    inlineErrorMessage: String?,
    isLoading: Boolean,
    onSignUp: () -> Unit,
    onSignIn: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onYandexSignIn: () -> Unit,
    contentPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    PixelPanel(modifier = modifier) {
        Column(
            modifier = Modifier.padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(
                    if (authMode == AuthMode.SignIn) {
                        R.string.auth_sign_in_title
                    } else {
                        R.string.auth_create_account_title
                    }
                ),
                color = CozyAuth.Ink,
                fontFamily = CozyAuth.PixelFont,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            PixelAuthTextField(
                value = email,
                onValueChange = onEmailChange,
                label = stringResource(R.string.auth_email_label),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = CozyAuth.InkSoft
                    )
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AUTH_EMAIL_FIELD_TEST_TAG)
            )
            Spacer(Modifier.height(10.dp))
            PixelAuthTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.auth_password_label),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = CozyAuth.InkSoft
                    )
                },
                trailingIcon = {
                    val description = if (passwordVisible) {
                        stringResource(R.string.auth_hide_password)
                    } else {
                        stringResource(R.string.auth_show_password)
                    }
                    IconButton(
                        onClick = onPasswordVisibilityChange,
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = description,
                            tint = CozyAuth.InkSoft
                        )
                    }
                },
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AUTH_PASSWORD_FIELD_TEST_TAG)
            )
            if (authMode == AuthMode.SignUp) {
                Spacer(Modifier.height(10.dp))
                PixelAuthTextField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = stringResource(R.string.auth_confirm_password_label),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CozyAuth.InkSoft
                        )
                    },
                    trailingIcon = {
                        val description = if (passwordVisible) {
                            stringResource(R.string.auth_hide_password)
                        } else {
                            stringResource(R.string.auth_show_password)
                        }
                        IconButton(
                            onClick = onPasswordVisibilityChange,
                            enabled = !isLoading
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = description,
                                tint = CozyAuth.InkSoft
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(AUTH_CONFIRM_PASSWORD_FIELD_TEST_TAG)
                )
            }
            if (inlineErrorMessage != null) {
                Text(
                    text = inlineErrorMessage,
                    color = MaterialTheme.colors.error,
                    fontFamily = CozyAuth.PixelFont,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag(AUTH_ERROR_MESSAGE_TEST_TAG)
                )
            }
            Spacer(Modifier.height(16.dp))
            PixelPrimaryButton(
                text = stringResource(
                    if (authMode == AuthMode.SignIn) {
                        R.string.auth_sign_in
                    } else {
                        R.string.auth_sign_up
                    }
                ),
                onClick = if (authMode == AuthMode.SignIn) onSignIn else onSignUp,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(
                        if (authMode == AuthMode.SignIn) {
                            AUTH_SIGN_IN_BUTTON_TEST_TAG
                        } else {
                            AUTH_SIGN_UP_BUTTON_TEST_TAG
                        }
                    )
            )
            PixelDividerWithText(
                text = stringResource(R.string.auth_divider_or),
                modifier = Modifier.padding(top = 18.dp, bottom = 12.dp)
            )
            PixelSocialButton(
                text = stringResource(R.string.auth_sign_in_google),
                onClick = onGoogleSignIn,
                enabled = !isLoading,
                leading = { SocialGlyph(stringResource(R.string.auth_google_glyph), Color(0xFFB94B37)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AUTH_GOOGLE_SIGN_IN_BUTTON_TEST_TAG)
            )
            Spacer(Modifier.height(10.dp))
            PixelSocialButton(
                text = stringResource(R.string.auth_sign_in_yandex),
                onClick = onYandexSignIn,
                enabled = !isLoading,
                leading = { SocialGlyph(stringResource(R.string.auth_yandex_glyph), Color(0xFFD83A2E)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AUTH_YANDEX_SIGN_IN_BUTTON_TEST_TAG)
            )
            Spacer(Modifier.height(14.dp))
            AuthModeSwitchLink(
                text = stringResource(
                    if (authMode == AuthMode.SignIn) {
                        R.string.auth_switch_to_sign_up
                    } else {
                        R.string.auth_existing_account_sign_in
                    }
                ),
                enabled = !isLoading,
                onClick = {
                    onAuthModeChange(
                        if (authMode == AuthMode.SignIn) {
                            AuthMode.SignUp
                        } else {
                            AuthMode.SignIn
                        }
                    )
                },
                modifier = Modifier.testTag(
                    if (authMode == AuthMode.SignIn) {
                        AUTH_SIGN_UP_BUTTON_TEST_TAG
                    } else {
                        AUTH_SIGN_IN_BUTTON_TEST_TAG
                    }
                )
            )
        }
    }
}

@Composable
private fun PixelAuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = {
            Text(
                text = label,
                fontFamily = CozyAuth.PixelFont
            )
        },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        enabled = enabled,
        singleLine = true,
        shape = RoundedCornerShape(4.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = CozyAuth.Ink,
            disabledTextColor = CozyAuth.InkSoft.copy(alpha = 0.55f),
            backgroundColor = CozyAuth.FieldCream,
            focusedBorderColor = CozyAuth.Terracotta,
            unfocusedBorderColor = CozyAuth.InputBorder,
            disabledBorderColor = CozyAuth.InputBorder.copy(alpha = 0.55f),
            cursorColor = CozyAuth.TerracottaDark,
            focusedLabelColor = CozyAuth.TerracottaDark,
            unfocusedLabelColor = CozyAuth.Hint,
            disabledLabelColor = CozyAuth.Hint.copy(alpha = 0.55f)
        )
    )
}

@Composable
private fun SocialGlyph(text: String, color: Color) {
    Box(
        modifier = Modifier
            .padding(end = 10.dp)
            .size(26.dp)
            .background(CozyAuth.CardCream, RoundedCornerShape(4.dp))
            .border(2.dp, color.copy(alpha = 0.75f), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontFamily = CozyAuth.PixelFont,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AuthModeSwitchLink(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = if (enabled) CozyAuth.TerracottaDark else CozyAuth.InkSoft.copy(alpha = 0.55f),
        fontFamily = CozyAuth.PixelFont,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .then(if (enabled) Modifier else Modifier.semantics { disabled() })
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    )
}

private fun Modifier.pixelScreenFrame(): Modifier = drawBehind {
    val outer = 4.dp.toPx()
    val inner = 8.dp.toPx()
    drawRect(CozyAuth.BrownOutline.copy(alpha = 0.32f), size = Size(size.width, outer))
    drawRect(
        CozyAuth.BrownOutline.copy(alpha = 0.32f),
        topLeft = Offset(0f, size.height - outer),
        size = Size(size.width, outer)
    )
    drawRect(CozyAuth.BrownOutline.copy(alpha = 0.32f), size = Size(outer, size.height))
    drawRect(
        CozyAuth.BrownOutline.copy(alpha = 0.32f),
        topLeft = Offset(size.width - outer, 0f),
        size = Size(outer, size.height)
    )
    drawRect(
        CozyAuth.MutedYellow.copy(alpha = 0.30f),
        topLeft = Offset(inner, inner),
        size = Size(outer, outer)
    )
    drawRect(
        CozyAuth.MutedYellow.copy(alpha = 0.30f),
        topLeft = Offset(size.width - inner - outer, inner),
        size = Size(outer, outer)
    )
    drawRect(
        CozyAuth.MutedYellow.copy(alpha = 0.30f),
        topLeft = Offset(inner, size.height - inner - outer),
        size = Size(outer, outer)
    )
    drawRect(
        CozyAuth.MutedYellow.copy(alpha = 0.30f),
        topLeft = Offset(size.width - inner - outer, size.height - inner - outer),
        size = Size(outer, outer)
    )
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
