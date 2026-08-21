package com.multipos.app.data

import android.content.Context
import com.multipos.app.data.entities.Usuario

object UserSessionStore {
    private const val PREFS = "multipos_session"
    private const val KEY_USER_ID = "authenticated_user_id"
    private const val KEY_SESSION_STARTED_AT = "session_started_at"
    private const val KEY_LAST_ACTIVITY_AT = "session_last_activity_at"
    private const val KEY_ROLE_LEGACY = "authenticated_user_role"

    fun set(context: Context, user: Usuario) {
        val now = System.currentTimeMillis()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_USER_ID, user.id)
            .putLong(KEY_SESSION_STARTED_AT, now)
            .putLong(KEY_LAST_ACTIVITY_AT, now)
            .remove(KEY_ROLE_LEGACY)
            .commit() // Cambiado de apply a commit para asegurar persistencia inmediata
    }

    fun userId(context: Context): Int {
        val id = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_USER_ID, 0)
        android.util.Log.d("UserSessionStore", "userId() -> $id")
        return id
    }

    fun sessionStartedAt(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_SESSION_STARTED_AT, 0L)

    fun lastActivityAt(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_LAST_ACTIVITY_AT, 0L)

    fun touchActivity(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_LAST_ACTIVITY_AT, System.currentTimeMillis())
            .apply()
    }

    fun isAuthenticated(context: Context): Boolean = userId(context) > 0

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(KEY_USER_ID)
            .remove(KEY_SESSION_STARTED_AT)
            .remove(KEY_LAST_ACTIVITY_AT)
            .remove(KEY_ROLE_LEGACY)
            .commit() // Cambiado de apply a commit
    }
}
