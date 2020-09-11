package com.revolut.rxdata.core.extensions

import com.revolut.rxdata.core.Data
import io.reactivex.Observable


fun <T> Observable<Data<T>>.extractContent(
    /**
     * If false, error will terminate the stream only if content isn't present,
     * if true error will terminate the stream regardless of the content
     */
    strictErrors: Boolean = true,
    /**
     * Original error will be replaced with the one returned by this lambda.
     * Normal usage is to return null for all known errors so that they don't terminate the stream.
     */
    consumeErrors: (Throwable, T?) -> Throwable? = { e, _ -> e }
): Observable<T> = map {
    val error = it.error?.let { error ->
        consumeErrors(error, it.content)
    }

    if (error != null && (strictErrors || it.content == null)) {
        throw error
    }
    it
}.filter { it.content != null }.map { it.content }

