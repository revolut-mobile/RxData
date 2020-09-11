package com.revolut.rxdata.core.extensions

import com.revolut.rxdata.core.Data
import io.reactivex.Observable


fun <T> Observable<Data<T>>.extractContent(
    /**
     * If true - then emits where loading == true will be filtered out (including errors emits).
     */
    filterLoading: Boolean = false,
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
): Observable<T> =
    filter {
        !(filterLoading && it.loading)
    }.map {
        val error = it.error?.let { error ->
            consumeErrors(error, it.content)
        }

        if (error != null && (strictErrors || it.content == null)) {
            throw error
        }
        it
    }.filter { it.content != null }.map { it.content }

