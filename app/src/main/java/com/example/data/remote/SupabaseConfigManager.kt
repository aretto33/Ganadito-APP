package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SupabaseConfigManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("supabase_config", Context.MODE_PRIVATE)

    private val realUrl = "https://ttgxzwszpmooaatuvdob.supabase.co"
    private val realKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InR0Z3h6d3N6cG1vb2FhdHV2ZG9iIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzcwNDI0NDgsImV4cCI6MjA5MjYxODQ0OH0.HcjSjEjRs_xGxMSr3JFcAmydF6qv080QydnUxE4yHIg"

    private val _supabaseUrl = MutableStateFlow(realUrl)
    val supabaseUrl: StateFlow<String> = _supabaseUrl.asStateFlow()

    private val _supabaseAnonKey = MutableStateFlow(realKey)
    val supabaseAnonKey: StateFlow<String> = _supabaseAnonKey.asStateFlow()

    private val _userId = MutableStateFlow(prefs.getString("user_id", "") ?: "")
    val userId: StateFlow<String> = _userId.asStateFlow()

    private val _userEmail = MutableStateFlow(prefs.getString("user_email", "") ?: "")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _accessToken = MutableStateFlow(prefs.getString("access_token", "") ?: "")
    val accessToken: StateFlow<String> = _accessToken.asStateFlow()

    // --- Identificadores de Base de Datos (Integers según esquema) ---
    private val _idUsuario = MutableStateFlow(prefs.getInt("id_usuario_db", -1))
    val idUsuario: StateFlow<Int> = _idUsuario.asStateFlow()

    private val _pkProductor = MutableStateFlow(prefs.getInt("pk_productor_db", -1))
    val pkProductor: StateFlow<Int> = _pkProductor.asStateFlow()

    private val _isOfflineOnly = MutableStateFlow(false)
    val isOfflineOnly: StateFlow<Boolean> = _isOfflineOnly.asStateFlow()

    fun saveSession(uid: String, email: String, token: String, dbId: Int = -1, prodId: Int = -1) {
        prefs.edit().apply {
            putString("user_id", uid)
            putString("user_email", email)
            putString("access_token", token)
            putInt("id_usuario_db", dbId)
            putInt("pk_productor_db", prodId)
            apply()
        }
        _userId.value = uid
        _userEmail.value = email
        _accessToken.value = token
        _idUsuario.value = dbId
        _pkProductor.value = prodId
    }

    fun clearSession() {
        prefs.edit().clear().apply()
        _userId.value = ""
        _userEmail.value = ""
        _accessToken.value = ""
        _idUsuario.value = -1
        _pkProductor.value = -1
    }

    fun isConfigured(): Boolean = true
    fun isLoggedIn(): Boolean = _userId.value.isNotEmpty()
    fun updateConfig(u: String, k: String) {}
    fun setOfflineOnly(o: Boolean) {}
}
