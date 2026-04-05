package com.github.jvsena42.mandacaru.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PreferencesDataSourceImpl(
    private val dataStore: DataStore<Preferences>
) : PreferencesDataSource {

    override suspend fun setString(key: PreferenceKeys, value: String) {
        dataStore.edit { preferences ->
            preferences[key.dataStoreKey] = value
        }
    }

    override suspend fun getString(key: PreferenceKeys, defaultValue: String): String {
        return dataStore.data
            .map { preferences -> preferences[key.dataStoreKey] ?: defaultValue }
            .first()
    }
}
