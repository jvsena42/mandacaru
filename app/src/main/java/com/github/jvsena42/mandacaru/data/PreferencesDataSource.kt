package com.github.jvsena42.mandacaru.data

interface PreferencesDataSource {
    suspend fun setString(key: PreferenceKeys, value: String)
    suspend fun getString(key: PreferenceKeys, defaultValue: String): String
    suspend fun setBoolean(key: PreferenceKeys, value: Boolean)
    suspend fun getBoolean(key: PreferenceKeys, defaultValue: Boolean): Boolean
}
