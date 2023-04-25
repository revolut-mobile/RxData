package com.revolut.rxdata.dod

import io.reactivex.Single
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.TestScheduler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.IOException

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

abstract class BaseDataObservableDelegateTest {

    val params: Params = 0
    val cachedDomain: Domain = "cached_domain_model"
    val backendException = IOException("HTTP 500. All tests are green!")

    lateinit var fromNetwork: (Params) -> Single<Domain>

    val fromNetworkScoped: DataObservableDelegate<Params, Domain>.(Params) -> Single<Domain> =
        { fromNetwork(it) }

    lateinit var toMemory: (Params, Domain) -> Unit
    lateinit var fromMemory: (Params) -> Domain
    lateinit var toStorage: (Params, Domain) -> Unit
    lateinit var fromStorage: (Params) -> Domain
    lateinit var dataObservableDelegate: DataObservableDelegate<Params, Domain>

    val computationScheduler: TestScheduler = TestScheduler()
    val ioScheduler: TestScheduler = TestScheduler()

    val memCache = hashMapOf<Params, Domain>()
    val storage = hashMapOf<Params, Domain>()

    @BeforeEach
    fun setUp() {
        fromNetwork = mock()
        toMemory = mock()
        fromMemory = mock()
        toStorage = mock()
        fromStorage = mock()

        dataObservableDelegate = DataObservableDelegate(
            fromNetwork = fromNetworkScoped,
            fromMemory = fromMemory,
            toMemory = toMemory,
            fromStorage = fromStorage,
            toStorage = toStorage
        )

        memCache.clear()
        storage.clear()

        whenever(fromMemory.invoke(any())).thenAnswer { invocation -> memCache[invocation.arguments[0]] }
        whenever(toMemory.invoke(any(), any())).thenAnswer { invocation ->
            memCache[invocation.arguments[0] as Params] = invocation.arguments[1] as Domain
            Unit
        }

        whenever(fromStorage.invoke(any())).thenAnswer { invocation -> storage[invocation.arguments[0]] }
        whenever(toStorage.invoke(any(), any())).thenAnswer { invocation ->
            storage[invocation.arguments[0] as Params] = invocation.arguments[1] as Domain
            Unit
        }

        RxJavaPlugins.setIoSchedulerHandler { ioScheduler }
        RxJavaPlugins.setComputationSchedulerHandler { computationScheduler }
    }

    @AfterEach
    fun afterEach() {
        RxJavaPlugins.reset()
    }


}