package com.github.jvsena42.mandacaru.data

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey

enum class PreferenceKeys(val dataStoreKey: Preferences.Key<String>) {
    CURRENT_NETWORK(stringPreferencesKey("CURRENT_NETWORK")),
    CURRENT_RPC_PORT(stringPreferencesKey("CURRENT_RPC_PORT")),
    PENDING_UTREEXO_SNAPSHOT(stringPreferencesKey("PENDING_UTREEXO_SNAPSHOT")),
    WALLET_BIRTHDAY_YEAR(stringPreferencesKey("WALLET_BIRTHDAY_YEAR")),
    WALLET_NEEDS_RESCAN(stringPreferencesKey("WALLET_NEEDS_RESCAN")),
    UPDATE_LAST_CHECK(stringPreferencesKey("UPDATE_LAST_CHECK")),
    UPDATE_LATEST_VERSION(stringPreferencesKey("UPDATE_LATEST_VERSION")),
    UPDATE_LATEST_APK_URL(stringPreferencesKey("UPDATE_LATEST_APK_URL")),
    UPDATE_SEEN_VERSION(stringPreferencesKey("UPDATE_SEEN_VERSION")),
    GEOIP_LAST_CHECK(stringPreferencesKey("GEOIP_LAST_CHECK")),
    GEOIP_DB_MONTH(stringPreferencesKey("GEOIP_DB_MONTH")),
    USE_ALSO_MOBILE_DATA(stringPreferencesKey("USE_ALSO_MOBILE_DATA")),
    ENABLE_ADVANCED_FEATURES(stringPreferencesKey("ENABLE_ADVANCED_FEATURES"))
}
