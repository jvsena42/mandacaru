package com.github.jvsena42.floresta_node.platform

/**
 * Platform-specific function to get the data directory for storing app data
 */
expect fun getDataDirectory(): String

/**
 * Platform-specific logging function
 */
expect fun platformLog(tag: String, message: String)
