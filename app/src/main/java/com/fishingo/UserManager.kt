package com.fishingo

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.fishingo.network.UserResponse

data class UserUi(
    val id: Int,
    val username: String,
    val email: String
)

object UserManager {
    private const val PREFS_NAME = "FishinGoPrefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_EMAIL = "email"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    private var prefs: SharedPreferences? = null

    private val _currentUser = mutableStateOf<UserUi?>(null)
    val currentUser: State<UserUi?> = _currentUser

    private val _isLoggedIn = mutableStateOf(false)
    val isLoggedIn: State<Boolean> = _isLoggedIn

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadUser()
    }

    private fun loadUser() {
        val loggedIn = prefs?.getBoolean(KEY_IS_LOGGED_IN, false) ?: false
        if (!loggedIn) {
            _currentUser.value = null
            _isLoggedIn.value = false
            return
        }

        val id = prefs?.getInt(KEY_USER_ID, -1) ?: -1
        val username = prefs?.getString(KEY_USERNAME, null)
        val email = prefs?.getString(KEY_EMAIL, null)

        if (id != -1 && username != null && email != null) {
            _currentUser.value = UserUi(id, username, email)
            _isLoggedIn.value = true
        } else {
            _currentUser.value = null
            _isLoggedIn.value = false
        }
    }

    fun setLoggedInUser(user: UserResponse) {
        // Save into SharedPreferences
        prefs?.edit()?.apply {
            putInt(KEY_USER_ID, user.id)
            putString(KEY_USERNAME, user.username)
            putString(KEY_EMAIL, user.email)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }

        // Update in-memory state (what Compose observes)
        _currentUser.value = UserUi(
            id = user.id,
            username = user.username,
            email = user.email
        )
        _isLoggedIn.value = true
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
}
