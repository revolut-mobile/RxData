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

@Deprecated(
    replaceWith = ReplaceWith("extractContent(filterLoading = false, strictErrors = false)"),
    message = "Dangerous method, ignores errors, replace with extractContent(filterLoading = false, strictErrors = false)"
)
fun <T> Observable<Data<T>>.extractData(): Observable<T> =
    filter { it.content != null }.map { it.content!! }

@Deprecated(
    message = "Use newer extractContent for content extraction, " +
            "extracting errors shouldn't be done separately"
)
fun <T> Observable<Data<T>>.extractError(): Observable<Data<T>> {
    return flatMap {
        if (it.error != null && it.content == null) {
            Observable.error(it.error)
        } else {
            Observable.just(it)
        }
    }
}

@Deprecated(
    replaceWith = ReplaceWith("extractContent(filterLoading = false, strictErrors = false)"),
    message = "Use newer extractContent for content extraction, " +
            "its non strict behaviour extracts errors when content is null"
)
fun <T> Observable<Data<T>>.extractDataOrError(): Observable<T> = extractError().extractData()

@Deprecated(
    replaceWith = ReplaceWith("extractContent(filterLoading = false, strictErrors = true)"),
    message = "Use newer extractContent for content extraction, " +
            "its strict behaviour extracts errors even when content is present"
)
fun <T> Observable<Data<T>>.extractErrorStrict(): Observable<Data<T>> = flatMap {
    if (it.error != null) {
        Observable.error(it.error)
    } else {
        Observable.just(it)
    }
}

@Deprecated(
    replaceWith = ReplaceWith("extractContent(filterLoading = true)"),
    message = "Use newer extractContent for content extraction, " +
            "filterLoading = true will filter out emits where loading is true. " +
            "Pay attention to error extraction policy"
)
fun <T> Observable<Data<T>>.extractDataIfLoaded(): Observable<T> {
    return filter { it.content != null && !it.loading }
        .map { it.content!! }
}
