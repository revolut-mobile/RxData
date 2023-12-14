package com.revolut.rxdata.dod_wrapper

import com.revolut.data.model.Data
import com.revolut.rxdata.dod.DataObservableDelegate
import com.revolut.rxdata.dod.LoadingStrategy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.rxSingle
import kotlinx.coroutines.withContext

/*
 * Copyright (C) 2023 Revolut
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

class DataObservableDelegateWrapper<Params : Any, Domain : Any>(
    fromNetwork: suspend DataObservableDelegateWrapper<Params, Domain>.(params: Params) -> Domain,
    fromMemory: (params: Params) -> Domain?,
    toMemory: (params: Params, Domain) -> Unit,
    fromStorage: suspend (params: Params) -> Domain?,
    toStorage: suspend (params: Params, Domain) -> Unit,
    onRemove: suspend (params: Params) -> Unit = { _ -> Unit },
) {
    private val inner = DataObservableDelegate<Params, Domain>(
        fromNetwork = { params ->
            rxSingle(DataObservableDelegateWrapperDispatchers.Unconfined) {
                fromNetwork(params)
            }
        },
        fromMemory = { params ->
            fromMemory(params)
        },
        toMemory = { params, domain ->
            toMemory(params, domain)
        },
        fromStorage = { params ->
            runBlocking {
                fromStorage(params)
            }
        },
        toStorage = { params, domain ->
            runBlocking {
                toStorage(params, domain)
            }
        },
        onRemove = { params ->
            runBlocking {
                onRemove(params)
            }
        }
    )

    @Deprecated(
        message = "please migrate to the method with the LoadingStrategy",
        replaceWith = ReplaceWith("fun observe(params: Params, loadingStrategy: LoadingStrategy)")
    )
    fun observe(params: Params, forceReload: Boolean = true): Flow<Data<Domain>> =
        observe(params, loadingStrategy = if (forceReload) LoadingStrategy.ForceReload else LoadingStrategy.Auto)

    fun observe(params: Params, loadingStrategy: LoadingStrategy): Flow<Data<Domain>> =
        inner.observe(
            params = params,
            loadingStrategy = loadingStrategy,
        ).asFlow()

    /**
     * Replaces the data in both caches (Memory, Persistent storage)
     * and emits an update.
     */
    suspend fun updateAll(params: Params, domain: Domain) =
        withContext(DataObservableDelegateWrapperDispatchers.IO) {
            inner.updateAll(params = params, domain = domain)
        }

    /**
     * Replaces the data and emits an update in persistent storage cache.
     *
     * /!\ Memory cache won't be dropped or replaced /!\
     */
    suspend fun updateStorage(params: Params, domain: Domain) =
        withContext(DataObservableDelegateWrapperDispatchers.IO) {
            inner.updateStorage(params = params, domain = domain)
        }

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
    fun notifyFromMemory(
        error: Throwable? = null,
        loading: Boolean = false,
        where: (Params) -> Boolean
    ) = inner.notifyFromMemory(error = error, loading = loading, where = where)

    suspend fun remove(params: Params) =
        withContext(DataObservableDelegateWrapperDispatchers.IO) {
            inner.remove(params = params)
        }

    suspend fun reload(params: Params, await: Boolean = false) = inner.reload(
        params = params,
        await = await,
    ).await()
}