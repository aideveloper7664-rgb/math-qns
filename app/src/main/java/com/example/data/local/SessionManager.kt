package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.remote.SupabaseClient

object SessionManager {
    private const val TAG = "AUTH_SESSION"
    private const val PREFS_NAME = "mathbaazi_session_prefs"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_DISPLAY_NAME = "display_name"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        loadSession()
    }

    fun saveSession(
        accessToken: String,
        refreshToken: String? = null,
        userId: String,
        email: String,
        displayName: String? = null
    ) {
        val editor = prefs?.edit() ?: return
        editor.putString(KEY_AUTH_TOKEN, accessToken)
        editor.putString(KEY_REFRESH_TOKEN, refreshToken)
        editor.putString(KEY_USER_ID, userId)
        editor.putString(KEY_USER_EMAIL, email)
        if (displayName != null) {
            editor.putString(KEY_DISPLAY_NAME, displayName)
        }
        editor.apply()

        SupabaseClient.authToken = accessToken
        SupabaseClient.currentUserId = userId
        SupabaseClient.currentUserEmail = email
        Log.d(TAG, "✅ Session saved and token active for $email (userId=$userId)")
    }

    fun loadSession(): Boolean {
        val token = prefs?.getString(KEY_AUTH_TOKEN, null)
        val userId = prefs?.getString(KEY_USER_ID, null)
        val email = prefs?.getString(KEY_USER_EMAIL, null)

        if (!token.isNullOrBlank() && !userId.isNullOrBlank()) {
            SupabaseClient.authToken = token
            SupabaseClient.currentUserId = userId
            SupabaseClient.currentUserEmail = email
            Log.d(TAG, "✅ Session restored for $email (token starting with: ${token.take(15)}...)")
            return true
        } else {
            Log.d(TAG, "⚠️ No session found in storage — user not logged in")
            return false
        }
    }

    fun clearSession() {
        prefs?.edit()?.clear()?.apply()
        SupabaseClient.authToken = null
        SupabaseClient.currentUserId = null
        Log.d(TAG, "Session cleared")
    }

    val userEmail: String?
        get() = prefs?.getString(KEY_USER_EMAIL, null)

    val displayName: String?
        get() = prefs?.getString(KEY_DISPLAY_NAME, null)

    val userId: String?
        get() = prefs?.getString(KEY_USER_ID, null)

    val authToken: String?
        get() = prefs?.getString(KEY_AUTH_TOKEN, null)
}
