package com.revolut.rxdata.core.extensions

import com.revolut.data.model.Data
import io.reactivex.Observable

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

fun <T> Observable<Data<T>>.doOnDataError(onError: (Throwable) -> Unit): Observable<Data<T>> =
    doOnNext { data ->
        data.error?.let { onError(it) }
    }

fun <T> Observable<Data<T>>.doOnLoaded(onNext: (T) -> Unit): Observable<Data<T>> =
    doOnNext { data ->
        val content = data.content
        if (!data.loading && data.error == null && content != null) {
            onNext(content)
        }
    }