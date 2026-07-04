package com.github.jvsena42.mandacaru.common

import kotlinx.coroutines.CancellationException

/**
 * Like [runCatching], but rethrows [CancellationException] so it never swallows
 * coroutine cancellation. Use inside suspend functions / coroutine builders.
 */
inline fun <T> runSuspendCatching(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
        Result.failure(e)
    }
