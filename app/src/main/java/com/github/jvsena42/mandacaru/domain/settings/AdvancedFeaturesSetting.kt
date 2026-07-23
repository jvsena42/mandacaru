package com.github.jvsena42.mandacaru.domain.settings

import com.github.jvsena42.mandacaru.BuildConfig
import com.github.jvsena42.mandacaru.data.PreferenceKeys
import com.github.jvsena42.mandacaru.data.PreferencesDataSource

/**
 * Advanced features are off for released builds and on everywhere else, so development
 * builds surface the network selector, peer flags and developer tools without a manual
 * opt-in on every fresh install.
 */
val ADVANCED_FEATURES_ENABLED_BY_DEFAULT = BuildConfig.DEBUG

/**
 * Single source of truth for the advanced-features preference, shared by every screen that
 * gates UI on it so they can never disagree about whether the feature is on.
 */
suspend fun PreferencesDataSource.isAdvancedFeaturesEnabled(): Boolean =
    getBoolean(PreferenceKeys.ENABLE_ADVANCED_FEATURES, ADVANCED_FEATURES_ENABLED_BY_DEFAULT)
