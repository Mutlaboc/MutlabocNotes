package com.example.mutlabocsnotes

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class AuthSessionSnapshot(
    val accessToken: String,
    val refreshToken: String,
    val email: String?
)

// Минимальный контракт хранения auth-сессии: его используют репозиторий и сетевой слой без знания Android-хранилища.
interface AuthSessionStore {
    fun saveSession(accessToken: String, refreshToken: String, email: String?)
    fun getSessionSnapshot(): AuthSessionSnapshot?
    fun saveSessionIfRefreshTokenMatches(
        expectedRefreshToken: String,
        accessToken: String,
        refreshToken: String,
        email: String?
    ): Boolean
    fun clearSessionIfRefreshTokenMatches(expectedRefreshToken: String): Boolean
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun getEmail(): String?
    fun hasSession(): Boolean
    fun clear()
}

// Production-хранилище сессии на EncryptedSharedPreferences.
class SessionManager(context: Context) : AuthSessionStore {

    private val appContext = context.applicationContext
    private val lock = Any()

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
    override fun saveSession(
        accessToken: String,
        refreshToken: String,
        email: String?
    ) {
        synchronized(lock) {
            prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putString(KEY_EMAIL, email)
                .commit()
        }
    }

    override fun getSessionSnapshot(): AuthSessionSnapshot? = synchronized(lock) {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
            ?.takeIf { it.isNotBlank() }
            ?: return@synchronized null
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
            ?.takeIf { it.isNotBlank() }
            ?: return@synchronized null

        AuthSessionSnapshot(
            accessToken = accessToken,
            refreshToken = refreshToken,
            email = prefs.getString(KEY_EMAIL, null)
        )
    }

    override fun saveSessionIfRefreshTokenMatches(
        expectedRefreshToken: String,
        accessToken: String,
        refreshToken: String,
        email: String?
    ): Boolean = synchronized(lock) {
        if (prefs.getString(KEY_REFRESH_TOKEN, null) != expectedRefreshToken) {
            return@synchronized false
        }

        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_EMAIL, email)
            .commit()
    }

    override fun clearSessionIfRefreshTokenMatches(expectedRefreshToken: String): Boolean = synchronized(lock) {
        if (prefs.getString(KEY_REFRESH_TOKEN, null) != expectedRefreshToken) {
            return@synchronized false
        }

        prefs.edit().clear().commit()
    }

    // Возвращает данные из текущего источника.
    override fun getAccessToken(): String? = synchronized(lock) {
        prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    // Возвращает данные из текущего источника.
    override fun getRefreshToken(): String? = synchronized(lock) {
        prefs.getString(KEY_REFRESH_TOKEN, null)
    }

    // Возвращает данные из текущего источника.
    override fun getEmail(): String? = synchronized(lock) {
        prefs.getString(KEY_EMAIL, null)
    }

    // Возвращает true, если в хранилище есть непустой access token.
    override fun hasSession(): Boolean = !getAccessToken().isNullOrBlank()

    // Очищает временные и сохранённые данные состояния.
    override fun clear() {
        synchronized(lock) {
            prefs.edit().clear().commit()
        }
    }

    private companion object {
        const val FILE_NAME = "secure_session"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EMAIL = "email"
    }
}
