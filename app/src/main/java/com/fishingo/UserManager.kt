package com.fishingo

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

object UserManager {
    private const val PREFS_NAME = "FishinGoPrefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_EMAIL = "email"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    private var prefs: SharedPreferences? = null

    private val _currentUser = mutableStateOf<User?>(null)
    val currentUser: State<User?> = _currentUser

    private val _isLoggedIn = mutableStateOf(false)
    val isLoggedIn: State<Boolean> = _isLoggedIn

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadUser()
    }

    private fun loadUser() {
        val isLoggedIn = prefs?.getBoolean(KEY_IS_LOGGED_IN, false) ?: false
        if (isLoggedIn) {
            val userId = prefs?.getString(KEY_USER_ID, null)
            val username = prefs?.getString(KEY_USERNAME, null)
            val email = prefs?.getString(KEY_EMAIL, null)

            if (userId != null && username != null && email != null) {
                _currentUser.value = User(userId, username, email)
                _isLoggedIn.value = true
            }
        }
    }

    fun login(username: String, email: String): Boolean {
        // In a real app, this would validate credentials with a backend
        val userId = generateUserId(email)

        prefs?.edit()?.apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
            putString(KEY_EMAIL, email)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }

        _currentUser.value = User(userId, username, email)
        _isLoggedIn.value = true
        return true
    }

    fun register(username: String, email: String, password: String): Boolean {
        // In a real app, this would create an account with a backend
        // For now, we'll just simulate a successful registration
        return login(username, email)
    }

    fun logout() {
        prefs?.edit()?.apply {
            remove(KEY_USER_ID)
            remove(KEY_USERNAME)
            remove(KEY_EMAIL)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }

        _currentUser.value = null
        _isLoggedIn.value = false
    }

    private fun generateUserId(email: String): String {
        return "user_${email.hashCode()}_${System.currentTimeMillis()}"
    }
}
