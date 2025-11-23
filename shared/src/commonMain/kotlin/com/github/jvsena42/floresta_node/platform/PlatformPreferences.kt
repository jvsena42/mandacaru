package com.github.jvsena42.floresta_node.platform

import com.github.jvsena42.floresta_node.data.PreferencesDataSource

/**
 * Platform-specific factory function to create PreferencesDataSource
 */
expect fun createPreferencesDataSource(): PreferencesDataSource
