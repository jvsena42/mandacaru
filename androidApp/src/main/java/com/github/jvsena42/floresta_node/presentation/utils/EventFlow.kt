package com.github.jvsena42.floresta_node.presentation.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

interface EventFlow<T> {
    val eventFlow: Flow<T>
    fun CoroutineScope.sendEvent(event: T): Job
}

class EventFlowImpl<T> : EventFlow<T> {
    private val eventChannel = Channel<T>(Channel.BUFFERED)
    override val eventFlow = eventChannel.receiveAsFlow()

    override fun CoroutineScope.sendEvent(event: T) = launch {
        eventChannel.send(event)
    }
}