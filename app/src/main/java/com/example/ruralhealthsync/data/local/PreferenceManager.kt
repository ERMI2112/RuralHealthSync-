package com.example.ruralhealthsync.data.local

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("rural_health_prefs", Context.MODE_PRIVATE)

    companion object {
        const val DEFAULT_SERVER_URL = "http://127.0.0.1/ruralhealth_api/"

        /** Maps API/DB values (admin, Admin, ADMIN) to CHW or ADMIN. */
        fun normalizeRole(role: String): String {
            return when (role.trim().uppercase()) {
                "ADMIN", "ADMINISTRATOR" -> "ADMIN"
                else -> "CHW"
            }
        }

        fun normalizeServerUrl(input: String): String {
            var url = input.trim()
            if (url.isEmpty()) return DEFAULT_SERVER_URL
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://$url"
            }
            if (!url.endsWith("/")) url += "/"
            if (!url.contains("ruralhealth_api")) {
                url = url.trimEnd('/') + "/ruralhealth_api/"
            }
            return url
        }
    }

    fun saveServerUrl(url: String) {
        prefs.edit().putString("server_url", normalizeServerUrl(url)).apply()
    }

    fun getServerUrl(): String {
        return prefs.getString("server_url", DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    }

    /** Host/IP only, for display on the login screen. */
    fun getServerUrlDisplay(): String {
        return getServerUrl()
            .removePrefix("http://")
            .removePrefix("https://")
            .removeSuffix("/ruralhealth_api/")
            .trimEnd('/')
    }

    fun saveUser(userId: Int, fullName: String, role: String) {
        val normalizedRole = normalizeRole(role)
        prefs.edit().apply {
            putInt("userId", userId)
            putString("fullName", fullName)
            putString("role", normalizedRole)
            apply()
        }
    }

    fun getUserId(): Int = prefs.getInt("userId", -1)

    fun getFullName(): String? = prefs.getString("fullName", null)

    fun getRole(): String = prefs.getString("role", "CHW") ?: "CHW"

    fun isAdmin(): Boolean = getRole() == "ADMIN"

    fun isLoggedIn(): Boolean = getUserId() != -1

    fun saveLastSyncTime(timeMillis: Long) {
        prefs.edit().putLong("lastSyncTime", timeMillis).apply()
    }

    fun getLastSyncTime(): Long = prefs.getLong("lastSyncTime", 0L)

    fun logout() {
        val serverUrl = getServerUrl()
        val themeMode = getThemeMode()
        val voiceInput = isVoiceInputEnabled()
        prefs.edit().clear().apply()
        saveServerUrl(serverUrl)
        saveThemeMode(themeMode)
        setVoiceInputEnabled(voiceInput)
    }

    // Theme Mode
    fun saveThemeMode(mode: Int) {
        prefs.edit().putInt("theme_mode", mode).apply()
    }

    fun getThemeMode(): Int {
        return prefs.getInt("theme_mode", androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    // Voice Input Toggle
    fun setVoiceInputEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("voice_input_enabled", enabled).apply()
    }

    fun isVoiceInputEnabled(): Boolean {
        return prefs.getBoolean("voice_input_enabled", false) // Off by default as requested
    }
}
