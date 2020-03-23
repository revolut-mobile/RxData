package com.revolut.rxdata.core.extensions

import com.revolut.rxdata.core.Data


fun <T> Data<T>.isEmpty(): Boolean = content == null

fun <T> Data<T>.isNotEmpty(): Boolean = content != null

/**
 * Maps data field of Data wrapper keeping error and loading fields.
 * Block is not invoked if data is null.
 */
inline fun <T, R> Data<T>.mapData(block: (T) -> R?): Data<R> = try {
    Data(
        content = content?.let { block(it) },
        error = error,
        loading = loading
    )
} catch (t: Throwable) {
    Data(
        content = null,
        error = t,
        loading = loading
    )
}
