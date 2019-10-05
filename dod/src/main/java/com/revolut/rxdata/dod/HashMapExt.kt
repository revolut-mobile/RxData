package com.revolut.rxdata.dod

import java.util.concurrent.ConcurrentHashMap

internal inline fun <K, V> ConcurrentHashMap<K, V>.getOrCreate(key: K, creator: () -> V): V =
    get(key) ?: creator().let { default ->
        putIfAbsent(key, default)
        default
    }
