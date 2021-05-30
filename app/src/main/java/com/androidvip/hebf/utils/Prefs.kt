package com.androidvip.hebf.utils

import android.content.Context
import android.content.SharedPreferences

sealed class BasePrefs(context: Context?, prefName: String) {
    val preferences: SharedPreferences

    init {
        requireNotNull(context) { "Context may not be null" }
        preferences = context.getSharedPreferences(prefName, 0)
    }

    fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }

    fun putString(key: String, value: String?) {
        preferences.edit().putString(key, value).apply()
    }

    fun getString(key: String?, defaultValue: String?): String {
        if (key == null || defaultValue == null) return ""
        return preferences.getString(key, defaultValue) ?: defaultValue
    }

    fun putBoolean(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return preferences.getBoolean(key, defaultValue)
    }

    fun putStringSet(key: String, values: Set<String>) {
        preferences.edit().putStringSet(key, values).apply()
    }

    fun getStringSet(key: String, defaultValues: Set<String>): MutableSet<String> {
        return preferences.getStringSet(key, defaultValues) ?: defaultValues.toMutableSet()
    }

    fun putInt(key: String, value: Int) {
        preferences.edit().putInt(key, value).apply()
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return try {
            val o = preferences.getLong(key, Long.MIN_VALUE)
            if (o == Long.MIN_VALUE) {
                preferences.getInt(key, defaultValue)
            } else {
                o.toInt()
            }
        } catch (e: Exception) {
            preferences.getInt(key, defaultValue)
        }
    }

    fun putLong(key: String, value: Long) {
        preferences.edit().putLong(key, value).apply()
    }

    fun getLong(key: String, defaultValue: Long): Long {
        return try {
            val o = preferences.getInt(key, Integer.MIN_VALUE)
            if (o == Integer.MIN_VALUE) {
                preferences.getLong(key, defaultValue)
            } else {
                o.toLong()
            }
        } catch (e: Exception) {
            preferences.getLong(key, defaultValue)
        }
    }

    inline fun edit(action: SharedPreferences.Editor.() -> Unit) {
        val editor = preferences.edit()
        action(editor)
        editor.apply()
    }
}

class Prefs(context: Context?) : BasePrefs(context, "Application")
class UserPrefs(context: Context) : BasePrefs(context, "Usuario")
class VipPrefs(context: Context) : BasePrefs(context, "VIP")
class VipStatePrefs(context: Context) : BasePrefs(context, "VipState")
class GbPrefs(context: Context) : BasePrefs(context, "GameBooster")