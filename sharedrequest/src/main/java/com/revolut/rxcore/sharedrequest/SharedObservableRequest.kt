package com.revolut.core.data.memory

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.*

class SharedObservableRequest<Key, Params, Result>(
    private val load: (params: Params) -> Observable<Result>
) {

    private val requests = HashMap<Key, Observable<Result>>()

    fun getOrLoad(key: Key, params: Params): Observable<Result> {
        return Observable
            .defer {
                synchronized(requests) {
                    requests[key]?.let { cachedShared ->
                        return@defer cachedShared
                    }

                    val newShared = load(params)
                        .observeOn(Schedulers.io())
                        .doFinally {
                            synchronized(requests) { requests.remove(key) }
                        }
                        .replay(1)
                        .refCount()

                    requests[key] = newShared
                    return@defer newShared
                }
            }
    }

}