package com.github.jvsena42.floresta_node.platform

import com.github.jvsena42.floresta_node.data.PreferenceKeys
import com.github.jvsena42.floresta_node.data.PreferencesDataSource
import java.util.prefs.Preferences

actual fun createPreferencesDataSource(): PreferencesDataSource {
    return DesktopPreferencesDataSource()
}

class DesktopPreferencesDataSource : PreferencesDataSource {
    private val prefs = Preferences.userNodeForPackage(DesktopPreferencesDataSource::class.java)

    override fun setString(key: PreferenceKeys, value: String) {
        prefs.put(key.name, value)
        prefs.flush()
    }

    override fun getString(key: PreferenceKeys, defaultValue: String): String {
        return prefs.get(key.name, defaultValue)
    }
}
