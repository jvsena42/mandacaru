package com.github.jvsena42.mandacaru.data.network

import kotlinx.coroutines.flow.StateFlow

/**
 * Process-wide WiFi-only / "also use mobile data" policy. Extracted from
 * [NetworkPolicyManager] so screens can depend on the signal without the
 * Android [android.content.Context] the manager needs.
 */
interface NetworkPolicy {
    /** True while WiFi-only is enforced but no WiFi network is currently bound. */
    val isWaitingForWifi: StateFlow<Boolean>

    /** Reads the current preference and (re)configures the process network binding. */
    fun apply()
}
