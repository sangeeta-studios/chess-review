package com.sangeetastudios.chessreview.data

import android.content.Context
import androidx.core.content.edit

class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** The user's own username; the board is oriented so their pieces are at the bottom. */
    var username: String
        get() = prefs.getString("username", "").orEmpty()
        set(value) = prefs.edit { putString("username", value.trim()) }

    var depth: Int
        get() = prefs.getInt("depth", DEFAULT_DEPTH)
        set(value) = prefs.edit { putInt("depth", value) }

    companion object {
        const val DEFAULT_DEPTH = 16
        val DEPTH_OPTIONS = listOf(12 to "Fast", 16 to "Standard", 20 to "Deep")
    }
}
