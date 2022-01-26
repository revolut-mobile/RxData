package com.revolut.flowdata.extensions

import com.revolut.data.model.Data
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/*
 * Copyright (C) 2022 Revolut
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

fun <T> Flow<Data<T>>.extractContent(
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

fun <T, R> Flow<Data<T>>.extractContent(
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
): Flow<R> = map { data ->
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

