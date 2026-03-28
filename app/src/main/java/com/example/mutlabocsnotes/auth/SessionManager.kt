package com.example.mutlabocsnotes.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "auth_session",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveSession(response: AuthResponseDto) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, response.accessToken)
            .putString(KEY_REFRESH_TOKEN, response.refreshToken)
            .putString(KEY_USER_ID, response.user.id)
            .putString(KEY_USER_EMAIL, response.user.email)
            .putString(KEY_USER_DISPLAY_NAME, response.user.displayName)
            .apply()
    }

    fun isLoggedIn(): Boolean =
        !prefs.getString(KEY_ACCESS_TOKEN, null).isNullOrBlank()

    fun getUserEmail(): String =
        prefs.getString(KEY_USER_EMAIL, "") ?: ""

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_USER_DISPLAY_NAME = "user_display_name"
    }
}
