package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SupabaseConfigManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("supabase_config", Context.MODE_PRIVATE)

    private val _supabaseUrl = MutableStateFlow(prefs.getString("supabase_url", "https://tu-proyecto.supabase.co") ?: "")
    val supabaseUrl: StateFlow<String> = _supabaseUrl.asStateFlow()

    private val _supabaseAnonKey = MutableStateFlow(prefs.getString("supabase_anon_key", "") ?: "")
    val supabaseAnonKey: StateFlow<String> = _supabaseAnonKey.asStateFlow()

    private val _userId = MutableStateFlow(prefs.getString("user_id", "") ?: "")
    val userId: StateFlow<String> = _userId.asStateFlow()

    private val _userEmail = MutableStateFlow(prefs.getString("user_email", "") ?: "")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _accessToken = MutableStateFlow(prefs.getString("access_token", "") ?: "")
    val accessToken: StateFlow<String> = _accessToken.asStateFlow()

    private val _isOfflineOnly = MutableStateFlow(prefs.getBoolean("offline_only", false))
    val isOfflineOnly: StateFlow<Boolean> = _isOfflineOnly.asStateFlow()

    fun updateConfig(url: String, anonKey: String) {
        prefs.edit().apply {
            putString("supabase_url", url)
            putString("supabase_anon_key", anonKey)
            apply()
        }
        _supabaseUrl.value = url
        _supabaseAnonKey.value = anonKey
    }

    fun saveSession(uid: String, email: String, token: String) {
        prefs.edit().apply {
            putString("user_id", uid)
            putString("user_email", email)
            putString("access_token", token)
            apply()
        }
        _userId.value = uid
        _userEmail.value = email
        _accessToken.value = token
    }

    fun clearSession() {
        prefs.edit().apply {
            remove("user_id")
            remove("user_email")
            remove("access_token")
            apply()
        }
        _userId.value = ""
        _userEmail.value = ""
        _accessToken.value = ""
    }

    fun setOfflineOnly(offline: Boolean) {
        prefs.edit().putBoolean("offline_only", offline).apply()
        _isOfflineOnly.value = offline
    }

    fun isConfigured(): Boolean {
        val url = _supabaseUrl.value
        val key = _supabaseAnonKey.value
        return url.isNotEmpty() && url.contains("supabase") && key.isNotEmpty()
    }

    fun isLoggedIn(): Boolean {
        return _userId.value.isNotEmpty()
    }
}
