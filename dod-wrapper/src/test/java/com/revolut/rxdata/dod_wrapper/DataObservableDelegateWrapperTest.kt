package com.revolut.rxdata.dod_wrapper

import app.cash.turbine.test
import com.revolut.data.model.Data
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
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

@Suppress("RemoveExplicitTypeArguments")
@ExperimentalCoroutinesApi
class DataObservableDelegateWrapperTest {

    private val networkResult = "I'm from network with correct param!"
    private val storageResult = "I'm from storage with correct param!"
    private val memCacheResult = "I'm from memCache with correct param!"
    private val param = "param"

    private val fromNetwork: suspend DataObservableDelegateWrapper<String, String>.(params: String) -> String = { p ->
        assert(p == param) { "Unexpected param '$p' in fromNetwork" }
        networkResult
    }
    private val fromMemory: (String) -> String? = { p ->
        assert(p == param) { "Unexpected param '$p' in fromMemory" }
        memCacheResult
    }
    private val fromStorage: suspend (params: String) -> String? = { p ->
        assert(p == param) { "Unexpected param '$p' in fromStorage" }
        storageResult
    }

    @Test
    fun `GIVEN no data in memory and storage WHEN observe THEN return loading and network data`() = runTest {
        val delegate = DataObservableDelegateWrapper<String, String>(
            fromNetwork = fromNetwork,
            fromMemory = { null },
            fromStorage = { null },
            toMemory = { _, _ -> },
            toStorage = { _, _ -> },
        )

        delegate.observe(params = param, forceReload = false).test {
            awaitItem() shouldBe Data(loading = true)
            awaitItem() shouldBe Data(content = networkResult)
        }
    }

    @Test
    fun `GIVEN storage throws WHEN observe THEN return loading, error and network data`() = runTest {
        val delegate = DataObservableDelegateWrapper<String, String>(
            fromNetwork = fromNetwork,
            fromMemory = { null },
            fromStorage = { throw IOException("No data") },
            toMemory = { _, _ -> },
            toStorage = { _, _ -> },
        )

        delegate.observe(params = param, forceReload = false).test {
            awaitItem() shouldBe Data(loading = true)
            awaitItem() shouldBe Data(error = IOException("No data"), loading = true)
            awaitItem() shouldBe Data(content = networkResult)
        }
    }

    @Test
    fun `GIVEN data in storage WHEN observe THEN return loading, storage and network data`() = runTest {
        val delegate = DataObservableDelegateWrapper<String, String>(
            fromNetwork = fromNetwork,
            fromMemory = { null },
            fromStorage = fromStorage,
            toMemory = { _, _ -> },
            toStorage = { _, _ -> },
        )

        delegate.observe(params = param, forceReload = false).test {
            awaitItem() shouldBe Data(loading = true)
            awaitItem() shouldBe Data(content = storageResult, loading = true)
            awaitItem() shouldBe Data(content = networkResult)
        }
    }

    @Test
    fun `GIVEN data in memory WHEN observe with forceReload=false THEN return memory data`() = runTest {
        val delegate = DataObservableDelegateWrapper<String, String>(
            fromNetwork = fromNetwork,
            fromMemory = fromMemory,
            fromStorage = fromStorage,
            toMemory = { _, _ -> },
            toStorage = { _, _ -> },
        )

        delegate.observe(params = param, forceReload = false).test {
            awaitItem() shouldBe Data(content = memCacheResult)
        }
    }

    @Test
    fun `GIVEN data in memory WHEN observe with forceReload=true THEN return memory and network data`() = runTest {
        val delegate = DataObservableDelegateWrapper<String, String>(
            fromNetwork = fromNetwork,
            fromMemory = fromMemory,
            fromStorage = fromStorage,
            toMemory = { _, _ -> },
            toStorage = { _, _ -> },
        )

        delegate.observe(params = param, forceReload = true).test {
            awaitItem() shouldBe Data(content = memCacheResult, loading = true)
            awaitItem() shouldBe Data(content = networkResult)
        }
    }

    @Test
    fun `GIVEN data in memory & network fails WHEN observe with forceReload=true THEN return memory and network error`() = runTest {
        val delegate = DataObservableDelegateWrapper<String, String>(
            fromNetwork = { throw IOException("Network error") },
            fromMemory = fromMemory,
            fromStorage = fromStorage,
            toMemory = { _, _ -> },
            toStorage = { _, _ -> },
        )

        delegate.observe(params = param, forceReload = true).test {
            awaitItem() shouldBe Data(content = memCacheResult, loading = true)
            awaitItem() shouldBe Data(content = memCacheResult, error = IOException("Network error"))
        }
    }

    // inspired by Kotest
    private infix fun <T, U : T> T.shouldBe(expected: U?) = assertEquals(expected, this)
}