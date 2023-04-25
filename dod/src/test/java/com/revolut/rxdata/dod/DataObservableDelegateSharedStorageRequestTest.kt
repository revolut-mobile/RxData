package com.revolut.rxdata.dod

import com.revolut.data.model.Data
import io.reactivex.Single
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicInteger

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

class DataObservableDelegateSharedStorageRequestTest : BaseDataObservableDelegateTest() {

    private val domain: Domain = "domain_model"

    @Suppress("CheckResult")
    @Test
    fun `WHEN memoryIsEmpty and dod is observed multiple times THEN fromStorage is called once`() {
        val counter = AtomicInteger(0)

        dataObservableDelegate = DataObservableDelegate(
            fromNetwork = fromNetworkScoped,
            fromMemory = fromMemory,
            toMemory = toMemory,
            fromStorageSingle = {
                Single.fromCallable {
                    counter.incrementAndGet()
                    Data(cachedDomain)
                }.delay(100L, MILLISECONDS, ioScheduler)
            },
            toStorage = toStorage
        )

        whenever(fromNetwork.invoke(eq(params))).thenReturn(Single.fromCallable { domain })

        dataObservableDelegate.observe(params = params, forceReload = true).test()
        dataObservableDelegate.observe(params = params, forceReload = true).test()
        dataObservableDelegate.observe(params = params, forceReload = true).test()

        ioScheduler.triggerActions()

        assertEquals(1, counter.get())
    }

}