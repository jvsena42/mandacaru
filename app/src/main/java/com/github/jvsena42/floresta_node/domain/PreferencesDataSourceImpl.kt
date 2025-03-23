package com.github.jvsena42.floresta_node.domain

import android.content.SharedPreferences
import com.github.jvsena42.floresta_node.data.PreferenceKeys
import com.github.jvsena42.floresta_node.data.PreferencesDataSource

class PreferencesDataSourceImpl(
    private val sharedPreferences: SharedPreferences
) : PreferencesDataSource {

    override fun setString(key: PreferenceKeys, value: String) {
        sharedPreferences.edit().putString(key.name, value).apply()
    }

    override fun getString(key: PreferenceKeys, defaultValue: String): String {
        return sharedPreferences.getString(key.name, defaultValue).orEmpty()
    }
}