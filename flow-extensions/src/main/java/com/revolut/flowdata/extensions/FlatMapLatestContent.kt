package com.revolut.flowdata.extensions

import com.revolut.data.model.Data
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

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

@ExperimentalCoroutinesApi
fun <T, R : Any> Flow<Data<T>>.flatMapLatestContent(block: (T) -> Flow<R>): Flow<Data<R>> =
    flatMapLatest { original ->
        val originalContent = original.content
        if (originalContent != null) {
            try {
                block(originalContent).map { transformed ->
                    transformed.toData(
                        loading = original.loading,
                        error = original.error
                    )
                }
            } catch (t: Throwable) {
                flowOf(
                    Data(
                        error = combineErrors(original.error, t),
                        loading = original.loading
                    )
                )
            }
        } else {
            flowOf(
                Data(
                    error = original.error,
                    loading = original.loading
                )
            )
        }
    }.distinctUntilChanged()


private fun <T> T?.toData(loading: Boolean = false, error: Throwable? = null): Data<T> =
    Data(
        content = this,
        error = error,
        loading = loading
    )