package com.revolut.rxdata.core.extensions

import com.revolut.rxdata.core.Data
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

fun <T> Observable<Data<T>>.extractData(): Observable<T> =
    filter { it.content != null }.map { it.content!! }

fun <T> Observable<Data<T>>.extractError(): Observable<Data<T>> {
    return flatMap {
        if (it.error != null && it.content == null) {
            Observable.error(it.error)
        } else {
            Observable.just(it)
        }
    }
}

fun <T> Observable<Data<T>>.extractDataOrError(): Observable<T> = extractError().extractData()

fun <T> Observable<Data<T>>.extractErrorStrict(): Observable<Data<T>> = flatMap {
    if (it.error != null) {
        Observable.error(it.error)
    } else {
        Observable.just(it)
    }
}

fun <T> Observable<Data<T>>.extractDataIfLoaded(): Observable<T> {
    return filter { it.content != null && !it.loading }
        .map { it.content!! }
}
