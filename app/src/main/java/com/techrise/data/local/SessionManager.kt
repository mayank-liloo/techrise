package com.techrise.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "techrise_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ID = "user_id"
    }

    fun saveSession(token: String, userId: String, email: String, role: String) {
        sharedPreferences.edit().apply {
            putString(KEY_JWT_TOKEN, token)
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_ROLE, role)
            apply()
        }
    }

    fun getToken(): String? {
        return sharedPreferences.getString(KEY_JWT_TOKEN, null)
    }

    fun getRole(): String? {
        return sharedPreferences.getString(KEY_USER_ROLE, null)
    }

    fun getEmail(): String? {
        return sharedPreferences.getString(KEY_USER_EMAIL, null)
    }

    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    fun clearSession() {
        sharedPreferences.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean {
        return getToken() != null
    }
}
