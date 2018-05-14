package com.jimliuxyz.maprunner.utils

import android.content.Context
import android.content.res.Configuration
import android.preference.PreferenceManager
import android.view.Surface
import android.view.WindowManager


/**
 * get the device ORIENTATION
 */
inline fun <T : Context> T.orientation(): Int {
    val display = (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
    val rotation = display.rotation

    if (rotation== Surface.ROTATION_0 || rotation== Surface.ROTATION_180)
        return Configuration.ORIENTATION_PORTRAIT
    else
        return Configuration.ORIENTATION_LANDSCAPE
}

object UUID62 {
    private val BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

    private fun num2UID(inNum: Long): String {
        check(inNum >= 0) { "number must >= 0" }

        var text = ""
        var num = inNum
        while (num > 0) {
            text = BASE62[(num % BASE62.length).toInt()] + text
            num /= BASE62.length
        }
        return text
    }

    private fun num2UID(num: Double): String {
        val split = num.toBigDecimal().toString().split("\\.".toRegex())
        return num2UID(split[0].toLong()) + if (split.size > 1) num2UID(split[1].toLong()) else ""
    }

    private inline fun String.fixLen(len: Int, padChar: Char): String {
        return substring(0, kotlin.math.min(len, length)).padEnd(len, '0')
    }

    fun randomUUID(len: Int = 6): String {
        return num2UID(Math.random()).fixLen(len, '0')
    }

    fun randomUUIDWithTime(len: Int = 6): String {
        var time = num2UID(System.currentTimeMillis()).fixLen(6, '0')
        var uuid = num2UID(Math.random()).fixLen(len, '0')
        return time + uuid
    }
}

inline fun <T : Context, reified R> T.getPref(key: String, def: R): R {

    var preference = PreferenceManager.getDefaultSharedPreferences(this)

    var value = when (def) {
        is Boolean -> preference.getBoolean(key, def) as R
        is String -> preference.getString(key, def) as R
        is Float -> preference.getFloat(key, def) as R
        is Int -> preference.getInt(key, def) as R
        is Long -> preference.getLong(key, def) as R
        else -> null
    }
    return value!!
}

/**
 * get sharedPreferences value by strResId of key
 */
inline fun <T : Context, reified R> T.getPref(strResId: Int, def: R): R {

    var key = resources.getString(strResId)
    var preference = PreferenceManager.getDefaultSharedPreferences(this)

    var value = when (def) {
        is Boolean -> preference.getBoolean(key, def) as R
        is String -> preference.getString(key, def) as R
        is Float -> preference.getFloat(key, def) as R
        is Int -> preference.getInt(key, def) as R
        is Long -> preference.getLong(key, def) as R
        else -> null
    }
    return value!!
}

inline fun <T : Context, reified R> T.setPref(key: String, value: R) {

    var preference = PreferenceManager.getDefaultSharedPreferences(this).edit()

    when (value) {
        is Boolean -> preference.putBoolean(key, value)
        is String -> preference.putString(key, value)
        is Float -> preference.putFloat(key, value)
        is Int -> preference.putInt(key, value)
        is Long -> preference.putLong(key, value)
        else -> null
    }
    preference.commit()
}


inline fun <T : Context, reified R> T.setPref(strResId: Int, value: R) {

    var key = resources.getString(strResId)
    var preference = PreferenceManager.getDefaultSharedPreferences(this).edit()

    when (value) {
        is Boolean -> preference.putBoolean(key, value)
        is String -> preference.putString(key, value)
        is Float -> preference.putFloat(key, value)
        is Int -> preference.putInt(key, value)
        is Long -> preference.putLong(key, value)
        else -> null
    }
    preference.commit()
}

