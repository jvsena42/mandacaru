package com.github.jvsena42.mandacaru.data

interface PreferencesDataSource {
    suspend fun setString(key: PreferenceKeys, value: String)
    suspend fun getString(key: PreferenceKeys, defaultValue: String): String
}
