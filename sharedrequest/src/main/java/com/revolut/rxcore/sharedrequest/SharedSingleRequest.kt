package com.revolut.core.data.memory

import io.reactivex.Single

class SharedSingleRequest<Key, Params, Result : Any>(
    private val load: (params: Params) -> Single<Result>
) {

    private val sharedObservableRequest: SharedObservableRequest<Key, Params, Result> =
        SharedObservableRequest { params ->
            load(params).toObservable()
        }

    fun getOrLoad(key: Key, params: Params): Single<Result> =
        sharedObservableRequest.getOrLoad(key, params).firstOrError()

}