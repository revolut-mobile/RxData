package com.revolut.rxdata.dod_wrapper.caching

import com.revolut.data.model.Data
import com.revolut.data.model.cache.CacheDefinition
import com.revolut.rxdata.dod_wrapper.DataObservableDelegateWrapper
import kotlinx.coroutines.flow.Flow

class CachingDataObservableDelegateWrapper<Params : Any, Domain : Any>(
    key: (params: Params) -> String,
    fromNetwork: suspend DataObservableDelegateWrapper<Params, Domain>.(params: Params) -> Domain,
    cache: CacheDefinition,
) {
    private val inner = DataObservableDelegateWrapper(
        fromNetwork = fromNetwork,
        fromMemory = { params -> cache.fromMemory(key(params)) },
        toMemory = { params, domain -> cache.toMemory(key(params), domain) },
        fromStorage = { params -> cache.fromStorage(key(params)) },
        toStorage = { params, domain -> cache.toStorage(key(params), domain) },
        onRemove = { params -> cache.remove<Domain>(key(params)) }
    )

    fun observe(params: Params, forceReload: Boolean = true): Flow<Data<Domain>> =
        inner.observe(params, forceReload)

    /**
     * Replaces the data in both caches (Memory, Persistent storage)
     * and emits an update.
     */
    suspend fun updateAll(params: Params, domain: Domain) =
        inner.updateAll(params, domain)

    /**
     * Replaces the data and emits an update in persistent storage cache.
     *
     * /!\ Memory cache won't be dropped or replaced /!\
     */
    suspend fun updateStorage(params: Params, domain: Domain) =
        inner.updateStorage(params, domain)

    /**
     * Replaces the data and emits an update in memory cache.
     */
    fun updateMemory(params: Params, domain: Domain) =
        inner.updateMemory(params = params, domain = domain)

    /**
     * Subscribers observing this DOD will be notified with
     * Data(fromMemory(params), loading = false, error = null).
     * @param where must return true if subscriber requires notification.
     */
    fun notifyFromMemory(error: Throwable? = null, loading: Boolean = false, where: (Params) -> Boolean) =
        inner.notifyFromMemory(error = error, loading = loading, where = where)

    suspend fun remove(params: Params) =
        inner.remove(params = params)

    suspend fun reload(params: Params, await: Boolean = false) =
        inner.reload(
            params = params,
            await = await,
        )
}
