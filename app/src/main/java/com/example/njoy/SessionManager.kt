package com.example.njoy

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson

object SessionManager {
    private const val PREF_NAME = "NJoySession"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_USER_DATA = "user_data"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun login(context: Context, accessToken: String, refreshToken: String, user: DataClasesApi.User) {
        val editor = getPreferences(context).edit()
        editor.putString(KEY_ACCESS_TOKEN, accessToken)
        editor.putString(KEY_REFRESH_TOKEN, refreshToken)
        // Serialize user to JSON
        val gson = Gson()
        editor.putString(KEY_USER_DATA, gson.toJson(user))
        editor.apply()
    }

    fun logout(context: Context) {
        val editor = getPreferences(context).edit()
        editor.clear()
        editor.apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        val token = getAccessToken(context)
        return token.isNotEmpty()
    }

    fun getAccessToken(context: Context): String {
        return getPreferences(context).getString(KEY_ACCESS_TOKEN, "") ?: ""
    }

    fun getRefreshToken(context: Context): String {
        return getPreferences(context).getString(KEY_REFRESH_TOKEN, "") ?: ""
    }

    fun getUser(context: Context): DataClasesApi.User? {
        val userJson = getPreferences(context).getString(KEY_USER_DATA, null)
        return if (userJson != null) {
            try {
                Gson().fromJson(userJson, DataClasesApi.User::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    fun getUserId(context: Context): Int {
        return getUser(context)?.id ?: -1
    }

    fun getUsername(context: Context): String {
        val user = getUser(context)
        return if (user != null) {
            "${user.nombre} ${user.apellidos}"
        } else {
            "Usuario"
        }
    }

    fun getEmail(context: Context): String {
        return getUser(context)?.email ?: ""
    }

    fun getUserRole(context: Context): String {
        return getUser(context)?.role ?: "user"
    }

    // Check if user can access admin features (event CRUD, ticket scanning)
    fun canManageEvents(context: Context): Boolean {
        val role = getUserRole(context)
        return role in listOf("admin", "owner", "promotor")
    }

    // Check if user is admin
    fun isAdmin(context: Context): Boolean {
        return getUserRole(context) == "admin"
    }

    // Check if user is banned
    fun isBanned(context: Context): Boolean {
        return getUser(context)?.is_banned ?: false
    }

    // Check if user is active
    fun isActive(context: Context): Boolean {
        return getUser(context)?.is_active ?: false
    }
    // Save new access token
    fun saveAccessToken(context: Context, token: String) {
        val editor = getPreferences(context).edit()
        editor.putString(KEY_ACCESS_TOKEN, token)
        editor.apply()
    }
    
    // Save new refresh token
    fun saveRefreshToken(context: Context, token: String) {
        val editor = getPreferences(context).edit()
        editor.putString(KEY_REFRESH_TOKEN, token)
        editor.apply()
    }

    // Update user data (keeps tokens intact)
    fun updateUser(context: Context, user: DataClasesApi.User) {
        val editor = getPreferences(context).edit()
        val gson = Gson()
        editor.putString(KEY_USER_DATA, gson.toJson(user))
        editor.apply()
    }
}