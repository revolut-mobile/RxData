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
     * Allows to substitute null content with some object by provided loading and error.
     */
    nullContentHandler: (loading: Boolean, error: Throwable?) -> T? = { _, _ -> null },
    /**
     * Original error will be replaced with the one returned by this lambda.
     * Normal usage is to return null for all known errors so that they don't terminate the stream.
     */
    consumeErrors: (error: Throwable, content: T?) -> Throwable? = { e, _ -> e }
): Observable<T> = map {
    val error = it.error?.let { error ->
        consumeErrors(error, it.content)
    }

    if (error != null && (strictErrors || it.content == null)) {
        throw error
    }

    if (it.content == null) {
        it.copy(content = nullContentHandler(it.loading, it.error))
    } else {
        it
    }
}.filter { it.content != null }.map { it.content }

