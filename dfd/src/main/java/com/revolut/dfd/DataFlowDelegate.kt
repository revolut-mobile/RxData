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
    private val toStorage: suspend (params: Params, Domain) -> Unit = { _, _ -> Unit }
) {

    private val flowsMap = ConcurrentHashMap<Params, MutableSharedFlow<Data<Domain>>>()

    @FlowPreview
    @ExperimentalCoroutinesApi
    suspend fun observe(params: Params, forceReload: Boolean): Flow<Data<Domain>> = flow {
        val fromMemory = fromMemory.invoke(params)
        val loading = fromMemory == null || forceReload
        val sharedFlow = sharedFlow(params)
        emitAll(
            getFromStorage(params = params, fromMemoryCache = fromMemory)
                .flatMapMerge { fromStorage ->
                    getFromNetworkIfNeeded(loading, fromStorage, params)
                }
                .catch {
                    //error in database
                    val dataWithLoading =
                        Data<Domain>(loading = true)
                    emit(dataWithLoading)
                    emitAll(sharedFlow)
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
        val sharedFlow = sharedFlow(params)
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
        }


    private suspend fun getFromNetwork(
        params: Params,
        storageData: Domain?
    ): Flow<Data<Domain>> {
        val flow = sharedFlow(params)
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

    private fun sharedFlow(params: Params): MutableSharedFlow<Data<Domain>> = flowsMap.getOrCreate(
        params,
        creator = { MutableSharedFlow() })
}