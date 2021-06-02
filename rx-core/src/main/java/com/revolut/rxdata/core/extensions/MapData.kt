package com.revolut.rxdata.core.extensions

import com.revolut.rxdata.core.Data
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single

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

fun <T, R> Flowable<Data<T>>.mapData(block: (T) -> R?): Flowable<Data<R>> =
    map { data -> data.mapData(block) }

fun <T, R> Observable<Data<T>>.mapData(block: (T) -> R?): Observable<Data<R>> =
    map { data -> data.mapData(block) }

fun <T, R> Single<Data<T>>.mapData(block: (T) -> R?): Single<Data<R>> =
    map { data -> data.mapData(block) }

fun <T, R> Maybe<Data<T>>.mapData(block: (T) -> R?): Maybe<Data<R>> =
    map { data -> data.mapData(block) }

fun <T> Flowable<Data<T>>.mapDataErrorToContent(block: (Throwable) -> T?): Flowable<Data<T>> =
    map { data -> data.mapDataErrorToContent(block) }

fun <T> Observable<Data<T>>.mapDataErrorToContent(block: (Throwable) -> T?): Observable<Data<T>> =
    map { data -> data.mapDataErrorToContent(block) }

fun <T> Single<Data<T>>.mapDataErrorToContent(block: (Throwable) -> T?): Single<Data<T>> =
    map { data -> data.mapDataErrorToContent(block) }

fun <T> Maybe<Data<T>>.mapDataErrorToContent(block: (Throwable) -> T?): Maybe<Data<T>> =
    map { data -> data.mapDataErrorToContent(block) }
