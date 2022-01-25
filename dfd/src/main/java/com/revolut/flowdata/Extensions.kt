package com.revolut.flowdata

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.onCompletion
import java.util.concurrent.ConcurrentHashMap

internal fun <T> Flow<T>.concatWith(nextFlow: Flow<T>) = onCompletion {
    emitAll(nextFlow)
}

internal inline fun <K, V> ConcurrentHashMap<K, V>.getOrCreate(key: K, creator: () -> V): V =
    get(key) ?: creator().let { default ->
        putIfAbsent(key, default)
        default
    }