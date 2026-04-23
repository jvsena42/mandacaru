package com.github.jvsena42.mandacaru.presentation.ui.screens.node

sealed interface NodeEvents {
    data object OnSnapshotApplied : NodeEvents
    data class OnShareAccumulator(val payload: String) : NodeEvents
}
