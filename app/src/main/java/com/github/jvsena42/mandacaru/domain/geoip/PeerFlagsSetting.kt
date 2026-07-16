package com.github.jvsena42.mandacaru.domain.geoip

import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource

/** Peer country flags are on unless the user turns them off in advanced settings. */
const val PEER_FLAGS_ENABLED_BY_DEFAULT = true

/**
 * Single source of truth for the peer-flags preference, shared by the lookup and the database
 * download so the two can never disagree about whether the feature is on.
 */
suspend fun PreferencesDataSource.isPeerFlagsEnabled(): Boolean =
    getBoolean(PreferenceKeys.GEOIP_FLAGS_ENABLED, PEER_FLAGS_ENABLED_BY_DEFAULT)
