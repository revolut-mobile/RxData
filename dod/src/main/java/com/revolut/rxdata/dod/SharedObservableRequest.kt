package com.revolut.rxdata.dod

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers.io
import java.util.concurrent.ConcurrentHashMap

/*
 * Copyright (C) 2019 Revolut
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

internal class SharedObservableRequest<Params : Any, Result>(
    private val load: (params: Params) -> Observable<Result>,
) {

    private val requests = ConcurrentHashMap<Params, Observable<Result>>()

    fun removeRequest(params: Params) {
        requests.remove(params)
    }

    fun getOrLoad(params: Params): Observable<Result> {
        return Observable
            .defer {
                requests.getOrCreate(params) {
                    load(params)
                        .doFinally { removeRequest(params) }
                        .replay(1)
                        .refCount()
                }
            }.subscribeOn(io())
    }

}