package com.revolut.flowdata

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.jetbrains.annotations.TestOnly

internal object DataFlowDelegateDispatchers {

    private var IO = Dispatchers.IO
    fun ioDispatcher() = IO

    @TestOnly
    fun setIoDispatcher(dispatcher: CoroutineDispatcher) {
        IO = dispatcher
    }
}