package com.github.jvsena42.mandacaru.domain.update

import android.net.Uri

/**
 * Represents the current state of the app update.
 */
sealed class UpdateState {

    object Idle : UpdateState()
	data class Downloading(
		val progress: Int
	) : UpdateState()
    data class ReadyToInstall(val uri: Uri) : UpdateState()
    object Available : UpdateState() // no download started yet
}
