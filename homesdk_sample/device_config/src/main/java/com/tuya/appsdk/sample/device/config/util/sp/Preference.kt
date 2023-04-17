package com.tuya.appsdk.sample.device.config.util.sp

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import kotlin.reflect.KProperty

/**
 * SP Util
 * @author aiwen <a href="mailto:developer@tuya.com"/>
 * @since 2/25/21 10:15 AM
 */
class Preference<T>(context: Context, val name: String, private val default: T) {


    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        private const val SP_NAME = "GATEWAY_LIST"
    }

    /**
     * Clear all data
     */
    fun clearPreference() {
        prefs.edit().clear().apply()
    }

    /**
     * Clear data by key
     */
    fun clearPreference(key: String) {
        prefs.edit().remove(key).apply()
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return getSharedPreferences(name, default)
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        putSharedPreferences(name, value)
    }

    @SuppressLint("CommitPrefEdits")
    private fun putSharedPreferences(name: String, value: T) = with(prefs.edit()) {
        when (value) {
            is Long -> putLong(name, value)
            is String -> putString(name, value)
            is Int -> putInt(name, value)
            is Boolean -> putBoolean(name, value)
            is Float -> putFloat(name, value)
            else -> throw IllegalArgumentException("This type can not be saved into Preferences")
        }.apply()
    }

    @Suppress("UNCHECKED_CAST")
    private fun getSharedPreferences(name: String, default: T): T = with(prefs) {
        val res: Any = when (default) {
            is Long -> getLong(name, default)
            is Int -> getInt(name, default)
            is Boolean -> getBoolean(name, default)
            is Float -> getFloat(name, default)
            is String -> getString(name, default) ?: ""
            else -> throw IllegalArgumentException("This type can be saved into Preferences")
        }
        return res as T
    }


}