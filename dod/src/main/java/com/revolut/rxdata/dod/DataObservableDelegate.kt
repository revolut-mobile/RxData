package com.revolut.rxdata.dod

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.util.concurrent.ConcurrentHashMap

/**
 * Provides a strategy of retrieving data from network and caching it.
 *
 * Support two levels of cache memory and storage (usually database or shared preferences)
 *
 */
class DataObservableDelegate<Params : Any?, Key : Any, Domain : Any>(
    fromNetwork: (params: Params) -> Single<Domain>,
    private val fromMemory: (key: Key, params: Params) -> Domain? = { _, _ -> null },
    private val toMemory: (key: Key, params: Params, Domain) -> Unit = { _, _, _ -> Unit },
    private val fromStorage: ((key: Key, params: Params) -> Domain?) = { _, _ -> null },
    private val toStorage: ((key: Key, params: Params, Domain) -> Unit) = { _, _, _ -> Unit },
    private val paramsKey: (params: Params) -> Key,
    private val remove: (key: Key, params: Params) -> Unit = { _, _ -> Unit },
    private val fromStorageSingle: ((key: Key, params: Params) -> Single<Data<Domain>>) =
        { key, params -> Single.fromCallable {
            Data(
                content = fromStorage(
                    key,
                    params
                )
            )
        } }

) {

    private val subjectsMap = ConcurrentHashMap<Key, Subject<Data<Domain>>>()
    private val sharedRequest: SharedSingleRequest<Key, Params, Domain> =
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
    fun observe(params: Params, forceReload: Boolean = true): Observable<Data<Domain>> = Observable.defer {
        val key = paramsKey(params)

        val memCache = fromMemory(key, params)
        val memoryIsEmpty = memCache == null
        val subject = subject(key)

        (fromStorageSingle(key, params)
            .doOnSuccess { cachedValue ->
                cachedValue.content?.let { value ->
                    toMemory(key, params, value)
                }
            }
            .toObservable()
            .subscribeOn(Schedulers.io())
            .takeIf { memoryIsEmpty } ?: Observable.just(Data(content = memCache)))
            .flatMap { cachedValue ->
                if (forceReload || memoryIsEmpty) {
                    val data = cachedValue.copy(loading = true)
                    subject.onNext(data)
                    fetchFromNetwork(cachedValue.content, key, params).startWith(data)
                } else {
                    Observable.just(cachedValue)
                }
            }
            .onErrorResumeNext { _: Throwable ->
                val data = Data(content = null, loading = true)
                subject.onNext(data)
                fetchFromNetwork(null, key, params).startWith(data)
            }
            .concatWith(subject(key).distinctUntilChanged())
    }

    /**
     * Replaces the data in both caches (Memory, Persistent storage)
     * and emits an update.
     */
    fun updateAll(params: Params, domain: Domain) {
        val key = paramsKey(params)
        toMemory(key, params, domain)
        toStorage(key, params, domain)
        subject(key).onNext(Data(content = domain))
    }

    /**
     * Replaces the data and emits an update in memory cache.
     */
    @Deprecated(message = "Don't use it as it could cause inconsistent state of the Delegate")
    fun updateMemory(params: Params, domain: Domain) {
        val key = paramsKey(params)
        toMemory(key, params, domain)
        subject(key).onNext(Data(content = domain))
    }

    /**
     * Replaces the data and emits an update in persistent storage cache.
     *
     * /!\ Memory cache won't be dropped or replaced /!\
     */
    @Deprecated(message = "Don't use it as it could cause inconsistent state of the Delegate")
    fun updateStorage(params: Params, domain: Domain) {
        val key = paramsKey(params)
        toStorage(key, params, domain)
        subject(key).onNext(Data(content = domain))
    }

    fun remove(params: Params) {
        val key = paramsKey(params)
        remove(key, params)
        subject(key).onNext(Data(content = null))
    }

    @Deprecated(message = "Don't use it as it could cause inconsistent state of the Delegate")
    fun notifyUpdated(params: Params, domain: Domain) {
        val key = paramsKey(params)
        subject(key).onNext(Data(content = domain))
    }

    private fun fetchFromNetwork(cachedData: Domain?, key: Key, params: Params): Observable<Data<Domain>> {
        return sharedRequest.getOrLoad(key, params)
            .map { domain ->
                toMemory(key, params, domain)
                toStorage(key, params, domain)

                val data = Data(content = domain)
                subject(key).onNext(data)
                data
            }
            .onErrorReturn { error ->
                val data = Data(content = cachedData, error = error)
                subject(key).onNext(data)
                data
            }
            .toObservable()
            .subscribeOn(Schedulers.io())
    }

    private fun subject(key: Key): Subject<Data<Domain>> = subjectsMap.getOrCreate(key, creator = { PublishSubject.create<Data<Domain>>().toSerialized() })

}
