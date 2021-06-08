package com.revolut.dfd

import com.revolut.flow_core.core.Data
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap

class DataFlowDelegate<Params : Any, Domain : Any>(
    private val fromNetwork: suspend (Params) -> Domain,
    private val fromMemory: (params: Params) -> Domain? = { _ -> null },
    private val toMemory: (params: Params, Domain) -> Unit = { _, _ -> Unit },
    private val fromStorage: suspend (params: Params) -> Domain? = { _ -> null },
    private val toStorage: suspend (params: Params, Domain) -> Unit = { _, _ -> Unit },
    private val onRemove: (params: Params) -> Unit = { _ -> Unit },
) {

    private val flowsMap = ConcurrentHashMap<Params, MutableSharedFlow<Data<Domain>>>()

    @FlowPreview
    @ExperimentalCoroutinesApi
    fun observe(params: Params, forceReload: Boolean): Flow<Data<Domain>> = flow {
        val fromMemory = fromMemory.invoke(params)
        val loading = fromMemory == null || forceReload
        emitAll(
            getFromStorage(params = params, fromMemoryCache = fromMemory)
                .flatMapMerge { fromStorage ->
                    getFromNetworkIfNeeded(loading, fromStorage, params)
                }
                .onStart {
                    emit(
                        Data(
                            content = fromMemory,
                            loading = loading
                        )
                    )
                }
                .distinctUntilChanged()
        )
    }


    @ExperimentalCoroutinesApi
    @FlowPreview
    private suspend fun getFromNetworkIfNeeded(
        loading: Boolean,
        fromStorage: Data<Domain>,
        params: Params
    ): Flow<Data<Domain>> {
        val sharedFlow = flow(params)
        return if (loading) {
            val data = fromStorage.copy(loading = true)
            sharedFlow.emit(data)
            flowOf(data)
                .concatWith(
                    merge(
                        getFromNetwork(
                            params = params,
                            storageData = fromStorage.content
                        ),
                        sharedFlow
                    )
                )
        } else {
            flowOf(fromStorage)
                .concatWith(sharedFlow)
        }
    }


    private suspend fun getFromStorage(
        params: Params,
        fromMemoryCache: Domain?
    ): Flow<Data<Domain>> =
        if (fromMemoryCache != null) {
            flowOf(Data(fromMemoryCache))
        } else {
            flow {
                val fromStorage = fromStorage.invoke(params)
                fromStorage?.let {
                    toMemory.invoke(params, fromStorage)
                }
                emit(Data(content = fromStorage))
            }.flowOn(DataFlowDelegateDispatchers.ioDispatcher())
                .catch { throwable ->
                    //error in database
                    val dataWithLoading =
                        Data<Domain>(error = throwable, loading = true)
                    emit(dataWithLoading)
                    emitAll(flow(params))
                }
        }


    private suspend fun getFromNetwork(
        params: Params,
        storageData: Domain?
    ): Flow<Data<Domain>> {
        val flow = flow(params)
        return flow {
            val fromNetwork = fromNetwork.invoke(params)
            toMemory.invoke(params, fromNetwork)
            toStorage.invoke(params, fromNetwork)
            val data = Data(content = fromNetwork)
            emit(data)
        }.flowOn(DataFlowDelegateDispatchers.ioDispatcher())
            .catch { error ->
                val errorData = Data(
                    content = storageData,
                    error = error
                )
                emit(errorData)
                flow.emit(errorData)
                emitAll(flow)
            }
    }

    /**
     * Replaces the data in both caches (Memory, Persistent storage)
     * and emits an update.
     */
    suspend fun updateAll(params: Params, domain: Domain) {
        toMemory(params, domain)
        toStorage(params, domain)
        flow(params).emit(Data(content = domain))
    }

    /**
     * Replaces the data and emits an update in memory cache.
     */
    suspend fun updateMemory(params: Params, domain: Domain) {
        toMemory(params, domain)
        flow(params).emit(Data(content = domain))
    }

    /**
     * Subscribers observing this DOD will be notified with
     * Data(fromMemory(params), loading = false, error = null).
     * @param where must return true if subscriber requires notification.
     */
    suspend fun notifyFromMemory(
        error: Throwable? = null,
        loading: Boolean = false,
        where: (Params) -> Boolean
    ) {
        flowsMap.forEach { (params, subject) ->
            if (where(params)) {
                subject.emit(Data(content = fromMemory(params), error = error, loading = loading))
            }
        }
    }

    /**
     * Replaces the data and emits an update in persistent storage cache.
     *
     * /!\ Memory cache won't be dropped or replaced /!\
     */
    suspend fun updateStorage(params: Params, domain: Domain) {
        toStorage(params, domain)
        flow(params).emit(Data(content = domain))
    }

    suspend fun remove(params: Params) {
        onRemove(params)
        flow(params).emit(Data(content = null))
    }

    private fun flow(params: Params): MutableSharedFlow<Data<Domain>> = flowsMap.getOrCreate(
        params,
        creator = { MutableSharedFlow() })
}