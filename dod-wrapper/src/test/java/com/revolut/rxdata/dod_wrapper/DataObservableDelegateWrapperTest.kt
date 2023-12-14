package com.revolut.rxdata.dod_wrapper

import app.cash.turbine.test
import com.revolut.data.model.Data
import com.revolut.rxdata.dod.LoadingStrategy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

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
    private val memoryResult = "I'm from memory with correct param!"
    private val param = "param"

    private val memoryCache = ConcurrentHashMap<String, String>()
    private val storage = ConcurrentHashMap<String, String>()

    @Test
    fun `GIVEN no data in memory and storage WHEN observe THEN return loading and network data`() = runTest {
        val delegate = createDelegate()

        delegate.observe(params = param, loadingStrategy = LoadingStrategy.Auto).test {
            awaitItem() shouldBe Data(loading = true)
            awaitItem() shouldBe Data(content = networkResult)
        }
        memoryCache[param] shouldBe networkResult
        storage[param] shouldBe networkResult
    }

    @Test
    fun `GIVEN storage throws WHEN observe THEN return loading, error and network data`() = runTest {
        val delegate = createDelegate(
            fromStorage = { throw IOException("No data") },
        )

        delegate.observe(params = param, loadingStrategy = LoadingStrategy.Auto).test {
            awaitItem() shouldBe Data(loading = true)
            awaitItem() shouldBe Data(error = IOException("No data"), loading = true)
            awaitItem() shouldBe Data(content = networkResult)
        }
        memoryCache[param] shouldBe networkResult
        storage[param] shouldBe networkResult
    }

    @Test
    fun `GIVEN data in storage WHEN observe THEN return loading, storage and network data`() = runTest {
        storage[param] = storageResult
        val delegate = createDelegate()

        delegate.observe(params = param, loadingStrategy = LoadingStrategy.Auto).test {
            awaitItem() shouldBe Data(loading = true)
            awaitItem() shouldBe Data(content = storageResult, loading = true)
            awaitItem() shouldBe Data(content = networkResult)
        }
        memoryCache[param] shouldBe networkResult
        storage[param] shouldBe networkResult
    }

    @Test
    fun `GIVEN data in memory WHEN observe with forceReload=false THEN return memory data`() = runTest {
        memoryCache[param] = memoryResult
        val delegate = createDelegate()

        delegate.observe(params = param, loadingStrategy = LoadingStrategy.Auto).test {
            awaitItem() shouldBe Data(content = memoryResult)
        }
    }

    @Test
    fun `GIVEN data in memory WHEN observe with forceReload=true THEN return memory and network data`() = runTest {
        memoryCache[param] = memoryResult
        val delegate = createDelegate()

        delegate.observe(params = param, loadingStrategy = LoadingStrategy.ForceReload).test {
            awaitItem() shouldBe Data(content = memoryResult, loading = true)
            awaitItem() shouldBe Data(content = networkResult)
        }
    }

    @Test
    fun `GIVEN data in memory & network fails WHEN observe with forceReload=true THEN return memory and network error`() = runTest {
        memoryCache[param] = memoryResult
        val delegate = createDelegate(
            fromNetwork = { throw IOException("Network error") },
        )

        delegate.observe(params = param, loadingStrategy = LoadingStrategy.ForceReload).test {
            awaitItem() shouldBe Data(content = memoryResult, loading = true)
            awaitItem() shouldBe Data(content = memoryResult, error = IOException("Network error"))
        }
    }

    @Test
    fun `GIVEN data in memory WHEN updateAll THEN update all places and notify`() = runTest {
        memoryCache[param] = memoryResult
        val delegate = createDelegate()
        val updatedValue = "Updated value"

        delegate.observe(params = param, loadingStrategy = LoadingStrategy.Auto).test {
            awaitItem() shouldBe Data(content = memoryResult)
            delegate.updateAll(param, updatedValue)
            awaitItem() shouldBe Data(content = updatedValue)
        }
        memoryCache[param] shouldBe updatedValue
        storage[param] shouldBe updatedValue
    }

    @Test
    fun `GIVEN data in memory WHEN updateMemory THEN update memory and notify`() = runTest {
        memoryCache[param] = memoryResult
        val delegate = createDelegate()
        val updatedValue = "Updated value"

        delegate.observe(params = param, loadingStrategy = LoadingStrategy.Auto).test {
            awaitItem() shouldBe Data(content = memoryResult)
            delegate.updateMemory(param, updatedValue)
            awaitItem() shouldBe Data(content = updatedValue)
        }
        memoryCache[param] shouldBe updatedValue
        storage[param] shouldBe null
    }

    @Test
    fun `GIVEN data in memory WHEN updateStorage THEN update storage and notify`() = runTest {
        memoryCache[param] = memoryResult
        val delegate = createDelegate()
        val updatedValue = "Updated value"

        delegate.observe(params = param, loadingStrategy = LoadingStrategy.Auto).test {
            awaitItem() shouldBe Data(content = memoryResult)
            delegate.updateStorage(param, updatedValue)
            awaitItem() shouldBe Data(content = updatedValue)
        }
        memoryCache[param] shouldBe memoryResult
        storage[param] shouldBe updatedValue
    }

    @Test
    fun `GIVEN data in memory WHEN notifyFromMemory THEN notify observers`() = runTest {
        memoryCache[param] = memoryResult
        val delegate = createDelegate()
        val updatedValue = "Updated value"

        delegate.observe(params = param, loadingStrategy = LoadingStrategy.Auto).test {
            awaitItem() shouldBe Data(content = memoryResult)
            memoryCache[param] = updatedValue
            delegate.notifyFromMemory { true }
            awaitItem() shouldBe Data(content = updatedValue)
        }
    }

    @Test
    fun `GIVEN data in memory WHEN remove THEN clear memory & storage & notify observers`() = runTest {
        memoryCache[param] = memoryResult
        storage[param] = storageResult
        val delegate = createDelegate()

        delegate.observe(params = param, loadingStrategy = LoadingStrategy.Auto).test {
            awaitItem() shouldBe Data(content = memoryResult)
            delegate.remove(param)
            awaitItem() shouldBe Data(content = null)
        }
        memoryCache[param] shouldBe null
        storage[param] shouldBe null
    }

    @Test
    fun `GIVEN data in memory WHEN reload THEN return loading and network data`() = runTest {
        memoryCache[param] = memoryResult
        storage[param] = storageResult
        val delegate = createDelegate()

        delegate.observe(params = param, loadingStrategy = LoadingStrategy.Auto).test {
            awaitItem() shouldBe Data(content = memoryResult)
            delegate.reload(param)
            awaitItem() shouldBe Data(content = memoryResult, loading = true)
            awaitItem() shouldBe Data(content = networkResult)
        }
        memoryCache[param] shouldBe networkResult
        storage[param] shouldBe networkResult
    }

    private fun createDelegate(
        fromNetwork: suspend DataObservableDelegateWrapper<String, String>.(params: String) -> String = defaultFromNetwork,
        fromMemory: (String) -> String? = defaultFromMemory,
        fromStorage: suspend (params: String) -> String? = defaultFromStorage,
        toMemory: (params: String, String) -> Unit = defaultToMemory,
        toStorage: suspend (params: String, String) -> Unit = defaultToStorage,
        onRemove: suspend (params: String) -> Unit = defaultOnRemove,
    ) = DataObservableDelegateWrapper<String, String>(
        fromNetwork = fromNetwork,
        fromMemory = fromMemory,
        fromStorage = fromStorage,
        toMemory = toMemory,
        toStorage = toStorage,
        onRemove = onRemove,
    )

    private val defaultFromNetwork: suspend DataObservableDelegateWrapper<String, String>.(params: String) -> String = { params ->
        assert(params == param) { "Unexpected param '$params' in fromNetwork" }
        networkResult
    }
    private val defaultFromMemory: (String) -> String? = { params ->
        memoryCache[params]
    }
    private val defaultFromStorage: suspend (params: String) -> String? = { params ->
        storage[params]
    }
    private val defaultToMemory: (params: String, String) -> Unit = { params, domain ->
        memoryCache[params] = domain
    }
    private val defaultToStorage: suspend (params: String, String) -> Unit = { params, domain ->
        storage[params] = domain
    }
    private val defaultOnRemove: suspend (params: String) -> Unit = { params ->
        memoryCache.remove(params)
        storage.remove(params)
    }
}

// inspired by Kotest
internal infix fun <T, U : T> T.shouldBe(expected: U?) = assertEquals(expected, this)
