package com.revolut.rxdata.core.extensions

import com.revolut.data.model.Data
import io.reactivex.Observable

fun <T> Observable<Data<T>>.extractContent(
    /**
     * Allows to substitute null content with some object by provided loading and error.
     */
    nullContentHandler: (loading: Boolean, consumedError: Throwable?) -> T? = { _, _ -> null },
    /**
     * Original error will be replaced with the one returned by this lambda.
     * Normal usage is to return null for all known errors so that they don't terminate the stream.
     */
    consumeErrors: (error: Throwable, content: T?) -> Throwable? = { e, _ -> e }
) = extractContent(
    contentMapper = { content, _, _ -> content },
    nullContentHandler = nullContentHandler,
    consumeErrors = consumeErrors
)

fun <T, R> Observable<Data<T>>.extractContent(
    /**
     * Allow to map content during the extraction
     */
    contentMapper: (content: T, loading: Boolean, consumedError: Throwable?) -> R,
    /**
     * Allows to substitute null content with some object by provided loading and error.
     */
    nullContentHandler: (loading: Boolean, consumedError: Throwable?) -> R? = { _, _ -> null },
    /**
     * Original error will be replaced with the one returned by this lambda.
     * Normal usage is to return null for all known errors so that they don't terminate the stream.
     */
    consumeErrors: (error: Throwable, content: T?) -> Throwable? = { e, _ -> e }
): Observable<R> = map { data ->
    val consumedError = data.error

    val error = data.error?.let { error ->
        consumeErrors(error, data.content)
    }

    if (error != null) {
        throw error
    }

    val content: R? = data.content?.let {
        contentMapper(it, data.loading, consumedError)
    } ?: nullContentHandler(data.loading, consumedError)

    Data<R>(
        content = content
    )
}.filter { it.content != null }
    .map { it.content!! }
    .distinctUntilChanged()

