package com.github.jvsena42.floresta_node.platform

import android.content.Context
import android.content.SharedPreferences
import com.github.jvsena42.floresta_node.data.PreferenceKeys
import com.github.jvsena42.floresta_node.data.PreferencesDataSource

private lateinit var androidContext: Context

fun initAndroidContext(context: Context) {
    androidContext = context.applicationContext
}

actual fun createPreferencesDataSource(): PreferencesDataSource {
    val sharedPreferences = androidContext.getSharedPreferences("floresta", Context.MODE_PRIVATE)
    return AndroidPreferencesDataSource(sharedPreferences)
}

class AndroidPreferencesDataSource(
    private val sharedPreferences: SharedPreferences
) : PreferencesDataSource {

    override fun setString(key: PreferenceKeys, value: String) {
        sharedPreferences.edit().putString(key.name, value).apply()
    }

    override fun getString(key: PreferenceKeys, defaultValue: String): String {
        return sharedPreferences.getString(key.name, defaultValue).orEmpty()
    }
}
