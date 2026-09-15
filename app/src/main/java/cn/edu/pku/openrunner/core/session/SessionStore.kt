package cn.edu.pku.openrunner.core.session

import android.content.Context

class SessionStore(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var userId: String?
        get() = preferences.getString(KEY_USER_ID, null)
        private set(value) = preferences.edit().putString(KEY_USER_ID, value).apply()

    var token: String?
        get() = preferences.getString(KEY_TOKEN, null)
        private set(value) = preferences.edit().putString(KEY_TOKEN, value).apply()

    val userName: String?
        get() = preferences.getString(KEY_USER_NAME, null)

    val department: String?
        get() = preferences.getString(KEY_DEPARTMENT, null)

    val isOffline: Boolean
        get() = preferences.getBoolean(KEY_OFFLINE, false)

    fun save(
        userId: String,
        token: String,
        offline: Boolean = false,
        userName: String? = null,
        department: String? = null
    ) {
        preferences.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_TOKEN, token)
            .putBoolean(KEY_OFFLINE, offline)
            .apply {
                if (userName == null) remove(KEY_USER_NAME) else putString(KEY_USER_NAME, userName)
                if (department == null) remove(KEY_DEPARTMENT) else putString(KEY_DEPARTMENT, department)
            }
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val FILE_NAME = "openrunner_session"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_TOKEN = "token"
        private const val KEY_OFFLINE = "offline"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_DEPARTMENT = "department"
    }
}
