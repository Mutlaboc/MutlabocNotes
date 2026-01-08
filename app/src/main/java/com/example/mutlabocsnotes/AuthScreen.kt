package com.example.mutlabocsnotes


import android.app.Activity
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
import androidx.compose.material.MaterialTheme          // ← вернули Material 1
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun AuthScreeen(onAuthenicated: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isPreview = LocalInspectionMode.current
    val auth = if (isPreview) null else FirebaseAuth.getInstance()
    val context = LocalContext.current
    val defaultWebClientId = stringResource(id = R.string.default_web_client_id)
    val googleSignInClient = remember {
        if (isPreview) null else {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(defaultWebClientId)
                .requestEmail()
                .build()
            GoogleSignIn.getClient(context, gso)
        }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (isPreview) return@rememberLauncherForActivityResult

        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
            Log.w("Auth", "Googlw sight-in cancelled or empty result")
            Toast.makeText(context, "Google sign-in cancelled", LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
    }
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken.isNullOrBlank()) {
                Log.e("Auth", "Google sign-in failed: missing idToken")
                Toast.makeText(context, "Google sign-in failed: missing token", LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth?.signInWithCredential(credential)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onAuthenicated()
                    } else {
                        Log.e("Auth", "Firebase auth failed", task.exception)
                        Toast.makeText(
                            context,
                            task.exception?.localizedMessage ?: "Firebase auth failed",
                            LENGTH_SHORT
                        ).show()
                    }
                }
        } catch (exception: ApiException) {
            Log.e("Auth", "Google sign in failed", exception)
            Toast.makeText(context, exception.localizedMessage ?: "Google sign in failed", LENGTH_SHORT)
                .show()
        }
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
                    Toast.makeText(context, "Введите корректный e-mail и пароль ≥ 6 символов", LENGTH_SHORT).show()
                    return@Button
                }
                if (!isPreview) {
                    auth?.signInWithEmailAndPassword(email.trim(), password)
                        ?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                onAuthenicated()
                            }
                            else {
                                Log.e("Auth", "Sign-up error", task.exception)
                                Toast.makeText(
                                    context,
                                    task.exception?.localizedMessage ?: "Ошибка входа",
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
                    Toast.makeText(context, "Введите корректный e-mail и пароль ≥ 6 символов", LENGTH_SHORT).show()
                    return@Button
            }
                if (!isPreview) {
                    auth?.createUserWithEmailAndPassword(email.trim(), password)
                        ?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                onAuthenicated()
                            } else {
                                Log.e("Auth", "Sign-up error", task.exception)
                                Toast.makeText(
                                    context,
                                    task.exception?.localizedMessage ?: "Ошибка регистрации",
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
                    val signIntent = googleSignInClient?.signInIntent
                    if (signIntent == null) {
                        Toast.makeText(context, "Google sigh in unavailable", LENGTH_SHORT).show()
                        return@Button
                    }
                    launcher.launch(signIntent)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign in with Google")
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
        AuthScreeen(onAuthenicated = {})
    }
}
