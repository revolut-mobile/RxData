package com.revolut.rxdata.dod

import io.reactivex.Single

internal class SharedSingleRequest<Key, Params, Result : Any>(
    private val load: (params: Params) -> Single<Result>
) {

    private val sharedObservableRequest: SharedObservableRequest<Key, Params, Result> =
        SharedObservableRequest { params ->
            load(params).toObservable()
        }

    fun getOrLoad(key: Key, params: Params): Single<Result> =
        sharedObservableRequest.getOrLoad(key, params).firstOrError()

}