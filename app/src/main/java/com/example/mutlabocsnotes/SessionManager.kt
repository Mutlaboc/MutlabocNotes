package com.example.mutlabocsnotes

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

// Управляет общим локальным состоянием, используемым во всём приложении.
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

    // Сохраняет текущие данные и фиксирует изменения.
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

    // Возвращает данные из текущего источника.
    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    // Возвращает данные из текущего источника.
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    // Возвращает данные из текущего источника.
    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)

    // Возвращает true, если в хранилище есть непустой access token.
    fun hasSession(): Boolean = !getAccessToken().isNullOrBlank()

    // Очищает временные и сохранённые данные состояния.
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
