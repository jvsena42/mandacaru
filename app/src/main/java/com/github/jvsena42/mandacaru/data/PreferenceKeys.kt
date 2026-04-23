package com.github.jvsena42.mandacaru.data

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey

enum class PreferenceKeys(val dataStoreKey: Preferences.Key<String>) {
    CURRENT_NETWORK(stringPreferencesKey("CURRENT_NETWORK")),
    CURRENT_RPC_PORT(stringPreferencesKey("CURRENT_RPC_PORT")),
    FAST_SYNC_ENABLED(stringPreferencesKey("FAST_SYNC_ENABLED")),
    PENDING_UTREEXO_SNAPSHOT(stringPreferencesKey("PENDING_UTREEXO_SNAPSHOT"))
}
