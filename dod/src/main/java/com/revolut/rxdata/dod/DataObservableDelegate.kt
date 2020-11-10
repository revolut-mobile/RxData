package com.revolut.rxdata.dod

import com.revolut.rxdata.core.Data
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Observable.concat
import io.reactivex.Observable.just
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.TimeUnit

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
    fromNetwork: DataObservableDelegate<Params, Domain>.(params: Params) -> Single<Domain>,
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
        SharedSingleRequest { params ->
            this.fromNetwork(params)
                .doOnSuccess { domain ->
                    failedNetworkRequests.remove(params)

                    toMemory(params, domain)
                    toStorage(params, domain)

                    val data = Data(content = domain)
                    subject(params).onNext(data)
                }

        }

    /**
     * Previously failed network requests will be reloaded on next observe
     * even if forceReload == false and memory is not empty
     */
    private val failedNetworkRequests = ConcurrentSkipListSet<Params>()

    /**
     * Requests data from network and subscribes to updates
     * (can be triggered by other subscribers or manual cache overrides)
     *
     * @param forceReload - if true network request will be made even if data exists in caches
     */
    @Suppress("RedundantLambdaArrow")
    fun observe(params: Params, forceReload: Boolean = true): Observable<Data<Domain>> =
        Observable.defer {
            val memCache = fromMemory(params)
            val memoryIsEmpty = memCache == null
            val subject = subject(params)
            val loading = forceReload || memoryIsEmpty || failedNetworkRequests.contains(params)

            val observable: Observable<Data<Domain>> = if (memCache != null) {
                concat(
                    just(Data(content = memCache, loading = loading)),
                    subject
                ).doOnSubscribe {
                    if (loading) {
                        subject.onNext(Data(content = memCache, loading = loading))
                        fetchFromNetwork(memCache, params)
                    }
                }
            } else {
                fromStorageSingle(params)
                    .doOnSuccess { cachedValue ->
                        cachedValue.content?.let { value ->
                            toMemory(params, value)
                        }
                        if (loading) {
                            fetchFromNetwork(cachedValue.content, params)
                        }
                    }
                    .subscribeOn(Schedulers.io())
                    .toObservable()
                    .onErrorResumeNext { error: Throwable ->
                        val data = Data(content = null, loading = true, error = error)
                        subject.onNext(data)

                        fetchFromNetwork(null, params)

                        concat(
                            just(data),
                            subject
                        )
                    }
                    .map { storageData ->
                        val data = storageData.copy(loading = loading)
                        subject.onNext(data)
                        data
                    }
                    .concatWith(subject)
                    .startWith(Data(null, loading = true))
            }

            observable.distinctUntilChanged()
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
    fun notifyFromMemory(
        error: Throwable? = null,
        loading: Boolean = false,
        where: (Params) -> Boolean
    ) {
        subjectsMap.forEach { (params, subject) ->
            if (where(params)) {
                subject.onNext(Data(content = fromMemory(params), error = error, loading = loading))
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

    fun reload(params: Params): Completable = Completable.fromAction {
        fetchFromNetwork(cachedData = fromMemory(params), params = params)
    }

    @Deprecated(
        message = "Don't use it as it could cause inconsistent state of the Delegate",
        replaceWith = ReplaceWith("notifyFromMemory { it == params }")
    )
    fun notifyUpdated(params: Params, domain: Domain) {
        subject(params).onNext(Data(content = domain))
    }

    @SuppressWarnings("CheckResult")
    /**
     * Suppressed because
     * - subscription is limited by 1 minute timeout.
     * - hot sharedRequest is internally restricted by refCount and will be
     * terminated after all cold sub-streams are disposed by timeout
     */
    private fun fetchFromNetwork(cachedData: Domain?, params: Params) {
        sharedRequest.getOrLoad(params)
            .toObservable()
            .subscribeOn(Schedulers.io())
            .timeout(FETCH_NETWORK_SUBSCRIPTION_TIMEOUT_MINUTES, TimeUnit.MINUTES, Schedulers.io())
            .subscribe({
                //all done in sharedRequest
            }, { error ->
                //error handling is here and not in sharedRequest
                //because timeout also generates an error that needs to be handled
                val data = Data(content = cachedData, error = error)
                failedNetworkRequests.add(params)
                subject(params).onNext(data)
            })
    }

    private fun subject(params: Params): Subject<Data<Domain>> = subjectsMap.getOrCreate(
        params,
        creator = { PublishSubject.create<Data<Domain>>().toSerialized() })


    companion object {
        private const val FETCH_NETWORK_SUBSCRIPTION_TIMEOUT_MINUTES = 1L
    }

}