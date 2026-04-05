package com.example.mutlabocsnotes

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

// Manages shared local state used across the app.
class SessionManager(context: Context) {

    private val appContext = context.applicationContext

    private val masterKey = MasterKey.Builder(appContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        appContext,
        FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Saves current data and persists changes.
    fun saveSession(
        accessToken: String,
        refreshToken: String,
        email: String?
    ) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_EMAIL, email)
            .apply()
    }

    // Returns data from the current source.
    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    // Returns data from the current source.
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    // Returns data from the current source.
    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)

    // Returns true when a non-empty access token exists in storage.
    fun hasSession(): Boolean = !getAccessToken().isNullOrBlank()

    // Clears temporary or persisted state values.
    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val FILE_NAME = "secure_session"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EMAIL = "email"
    }
}
