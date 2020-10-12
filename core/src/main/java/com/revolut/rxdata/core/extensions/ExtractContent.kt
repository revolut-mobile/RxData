package com.revolut.rxdata.core.extensions

import com.revolut.rxdata.core.Data
import io.reactivex.Observable


fun <T, R> Observable<Data<T>>.extractContent(
    contentMapper: (content: T, loading: Boolean, consumedError: Throwable?) -> R = { content, _, _ -> content as R },
    /**
     * Allows to substitute null content with some object by provided loading and error.
     */
    nullContentHandler: (loading: Boolean, consumedError: Throwable?) -> R? = { _, _ -> null },
    /**
     * Original error will be replaced with the one returned by this lambda.
     * Normal usage is to return null for all known errors so that they don't terminate the stream.
     */
    consumeErrors: (error: Throwable, content: T?) -> Throwable? = { e, _ -> e }
): Observable<R> = map {
    val consumedError = it.error

    val error = it.error?.let { error ->
        consumeErrors(error, it.content)
    }

    if (error != null) {
        throw error
    }

    val content: R? = if (it.content == null) {
        nullContentHandler(it.loading, consumedError)
    } else {
        contentMapper(it.content, it.loading, consumedError)
    }

    Data<R>(
        content = content
    )
}.filter { it.content != null }.map { it.content }

