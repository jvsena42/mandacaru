package com.github.jvsena42.floresta_node.data

interface PreferencesDataSource {
    fun setString(key: PreferenceKeys, value: String)
    fun getString(key: PreferenceKeys, defaultValue: String) : String
}