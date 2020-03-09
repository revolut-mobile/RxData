package com.revolut.rxdata.dod

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
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

/**
 * Provides a strategy of retrieving data from network and caching it.
 *
 * Support two levels of cache memory and storage (usually database or shared preferences)
 *
 */
class DataObservableDelegate<Params : Any, Domain : Any> constructor(
    fromNetwork: (params: Params) -> Single<Domain>,
    private val fromMemory: (params: Params) -> Domain? = { _ -> null },
    private val toMemory: (params: Params, Domain) -> Unit = { _, _ -> Unit },
    private val fromStorage: ((params: Params) -> Domain?) = { _ -> null },
    private val toStorage: ((params: Params, Domain) -> Unit) = { _, _ -> Unit },
    private val onRemove: (params: Params) -> Unit = { _ -> Unit },
    private val fromStorageSingle: ((params: Params) -> Single<Data<Domain>>) =
        { params -> Single.fromCallable { Data(content = fromStorage(params)) } }

) {

    private val subjectsMap = ConcurrentHashMap<Params, Subject<Data<Domain>>>()
    private val sharedRequest: SharedSingleRequest<Params, Domain> =
        SharedSingleRequest(fromNetwork)

    /**
     * Requests data from network and subscribes to updates
     * (can be triggered by other subscribers or manual cache overrides)
     *
     * @param forceReload - if true network request will be made even if data exists in caches
     *
     * For details:
     * https://revolut.atlassian.net/wiki/spaces/BD/pages/971374735/Offline+mode+strategy
     */
    @Suppress("RedundantLambdaArrow")
    fun observe(params: Params, forceReload: Boolean = true): Observable<Data<Domain>> =
        Observable.defer {
            val memCache = fromMemory(params)
            val memoryIsEmpty = memCache == null
            val subject = subject(params)

            (fromStorageSingle(params)
                .doOnSuccess { cachedValue ->
                    cachedValue.content?.let { value ->
                        toMemory(params, value)
                    }
                }
                .toObservable()
                .subscribeOn(Schedulers.io())
                .takeIf { memoryIsEmpty } ?: Observable.just(Data(content = memCache)))
                .flatMap { cachedValue ->
                    if (forceReload || memoryIsEmpty) {
                        val data = cachedValue.copy(loading = true)
                        subject.onNext(data)
                        fetchFromNetwork(cachedValue.content, params).startWith(data)
                    } else {
                        Observable.just(cachedValue)
                    }
                }
                .onErrorResumeNext { _: Throwable ->
                    val data = Data(content = null, loading = true)
                    subject.onNext(data)
                    fetchFromNetwork(null, params).startWith(data)
                }
                .concatWith(subject.distinctUntilChanged())
        }

    /**
     * Replaces the data in both caches (Memory, Persistent storage)
     * and emits an update.
     */
    fun updateAll(params: Params, domain: Domain) {
        toMemory(params, domain)
        toStorage(params, domain)
        subject(params).onNext(Data(content = domain))
    }

    /**
     * Replaces the data and emits an update in memory cache.
     */
    fun updateMemory(params: Params, domain: Domain) {
        toMemory(params, domain)
        subject(params).onNext(Data(content = domain))
    }

    /**
     * Subscribers observing this DOD will be notified with
     * Data(fromMemory(params), loading = false, error = null).
     * @param where must return true if subscriber requires notification.
     */
    fun notifyFromMemory(where: (Params) -> Boolean) {
        subjectsMap.forEach { (params, subject) ->
            if (where(params)) {
                subject.onNext(Data(content = fromMemory(params)))
            }
        }
    }

    /**
     * Replaces the data and emits an update in persistent storage cache.
     *
     * /!\ Memory cache won't be dropped or replaced /!\
     */
    fun updateStorage(params: Params, domain: Domain) {
        toStorage(params, domain)
        subject(params).onNext(Data(content = domain))
    }

    fun remove(params: Params) {
        onRemove(params)
        subject(params).onNext(Data(content = null))
    }

    @Deprecated(message = "Don't use it as it could cause inconsistent state of the Delegate")
    fun notifyUpdated(params: Params, domain: Domain) {
        subject(params).onNext(Data(content = domain))
    }

    private fun fetchFromNetwork(cachedData: Domain?, params: Params): Observable<Data<Domain>> {
        return sharedRequest.getOrLoad(params)
            .map { domain ->
                toMemory(params, domain)
                toStorage(params, domain)

                val data = Data(content = domain)
                subject(params).onNext(data)
                data
            }
            .onErrorReturn { error ->
                val data = Data(content = cachedData, error = error)
                subject(params).onNext(data)
                data
            }
            .toObservable()
            .subscribeOn(Schedulers.io())
    }

    private fun subject(params: Params): Subject<Data<Domain>> = subjectsMap.getOrCreate(
        params,
        creator = { PublishSubject.create<Data<Domain>>().toSerialized() })

}