package com.revolut.rxdata.core.extensions

import com.revolut.data.model.Data


/*
 * Copyright (C) 2020 Revolut
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

inline fun <T> Data<T>.mapDataErrorToContent(block: (Throwable) -> T?): Data<T> {
    val error = this.error

    return if (error != null) {
        try {
            Data(
                content = block(error),
                error = null,
                loading = loading
            )
        } catch (t: Throwable) {
            Data(
                content = null,
                error = t,
                loading = loading
            )
        }
    } else {
        this
    }
}
