package com.revolut.rxdata.dod

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.*

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

internal class SharedObservableRequest<Params, Result>(
    private val load: (params: Params) -> Observable<Result>
) {

    private val requests = HashMap<Params, Observable<Result>>()

    fun getOrLoad(params: Params): Observable<Result> {
        return Observable
            .defer {
                synchronized(requests) {
                    requests[params]?.let { cachedShared ->
                        return@defer cachedShared
                    }

                    val newShared = load(params)
                        .observeOn(Schedulers.io())
                        .doFinally {
                            synchronized(requests) { requests.remove(params) }
                        }
                        .replay(1)
                        .refCount()

                    requests[params] = newShared
                    return@defer newShared
                }
            }
    }

}