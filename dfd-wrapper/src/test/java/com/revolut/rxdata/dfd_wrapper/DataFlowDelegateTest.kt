package com.revolut.rxdata.dfd_wrapper

import com.revolut.data.model.Data
import com.revolut.flowdata.extensions.extractContent
import com.revolut.rxdata.dfd_wrapper.DataFlowDelegateTest.SuspendingFrom
import com.revolut.rxdata.dod.DodGlobal
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.TestScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.only
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

typealias Params = Int
typealias Domain = String

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

@ExperimentalCoroutinesApi
class DataFlowDelegateTest {

    private val params: Params = 0

    private val cachedDomain: Domain = "cached_domain_model"

    private val domain: Domain = "domain_model"
    private val domain2: Domain = "domain_model_2"

    private val backendException = RuntimeException("HTTP 500. All tests are green!")

    // workaround for https://github.com/mockito/mockito-kotlin/issues/398
    private fun interface SuspendingFrom {
        suspend fun invoke(params: Params): Domain
    }

    private fun interface SuspendingTo {
        suspend fun invoke(params: Params, domain: Domain)
    }

    private lateinit var fromNetwork: SuspendingFrom

    private lateinit var toMemory: (Params, Domain) -> Unit

    private lateinit var fromMemory: (Params) -> Domain

    private lateinit var toStorage: SuspendingTo

    private lateinit var fromStorage: SuspendingFrom

    private lateinit var dataFlowDelegate: DataFlowDelegate<Params, Domain>

    private val testDispatcher = UnconfinedTestDispatcher()
    private val computationScheduler: TestScheduler = TestScheduler()
    private val ioScheduler: TestScheduler = TestScheduler()

    private val memCache = hashMapOf<Params, Domain>()
    private val storage = hashMapOf<Params, Domain>()

    @BeforeEach
    fun setUp() {
        fromNetwork = mock()
        toMemory = mock()
        fromMemory = mock()
        toStorage = mock()
        fromStorage = mock()

        dataFlowDelegate = DataFlowDelegate(
            fromNetwork = { params -> fromNetwork.invoke(params) },
            fromMemory = fromMemory,
            toMemory = toMemory,
            fromStorage = { params -> fromStorage.invoke(params) },
            toStorage = { params, domain -> toStorage.invoke(params, domain) }
        )

        memCache.clear()
        storage.clear()

        whenever(fromMemory.invoke(any())) doAnswer { invocation -> memCache[invocation.arguments[0]] }
        whenever(toMemory.invoke(any(), any())) doAnswer { invocation ->
            memCache[invocation.arguments[0] as Params] = invocation.arguments[1] as Domain
        }

        fromStorage.stub {
            onBlocking { invoke(any()) } doAnswer { invocation -> storage[invocation.arguments[0]] }
        }
        toStorage.stub {
            onBlocking { invoke(any(), any()) } doAnswer { invocation ->
                storage[invocation.arguments[0] as Params] = invocation.arguments[1] as Domain
            }
        }

        RxJavaPlugins.setIoSchedulerHandler { ioScheduler }
        RxJavaPlugins.setComputationSchedulerHandler { computationScheduler }
        DataFlowDelegateDispatchers.setIoDispatcher(testDispatcher)
    }

    @AfterEach
    fun afterEach() {
        RxJavaPlugins.reset()
    }

    private fun fastTest(testBody: suspend TestScope.() -> Unit) = runTest(
        context = testDispatcher,
        dispatchTimeoutMs = 1_000,
        testBody = testBody,
    )

    @Test
    fun `FORCE observing data when memory cache IS EMPTY and storage IS EMPTY`() = fastTest {
        whenever(fromNetwork.invoke(eq(params))).thenReturn(domain)

        val testObserver =
            dataFlowDelegate.observe(params = params, forceReload = true).test(backgroundScope)

        testObserver.assertValueCount(1)
        testObserver.assertValueAt(0, Data(null, error = null, loading = true))

        ioScheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS)
        testObserver.assertValueCount(2)

        testObserver.assertValueAt(1, Data(domain, error = null, loading = false))

        verify(fromNetwork, only()).invoke(eq(params))
        verify(fromMemory, only()).invoke(eq(params))
        verify(toMemory, only()).invoke(eq(params), eq(domain))
        verify(fromStorage, only()).invoke(eq(params))
        verify(toStorage, only()).invoke(eq(params), eq(domain))
    }

    @Test
    fun `FORCE observing data when memory cache IS EMPTY and storage IS NOT EMPTY`() = fastTest {
        whenever(fromNetwork.invoke(eq(params))).thenReturn(domain)
        storage[params] = cachedDomain

        val testObserver =
            dataFlowDelegate.observe(params = params, forceReload = true).test(backgroundScope)

        testObserver.assertValueCount(1)
        testObserver.assertValueAt(0, Data(null, error = null, loading = true))

        ioScheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS)

        testObserver.assertValueCount(3)

        testObserver.assertValueAt(1, Data(cachedDomain, error = null, loading = true))
        testObserver.assertValueAt(2, Data(domain, error = null, loading = false))

        verify(fromNetwork, only()).invoke(eq(params))
        verify(fromMemory, only()).invoke(eq(params))
        verify(toMemory).invoke(eq(params), eq(cachedDomain))
        verify(toMemory).invoke(eq(params), eq(domain))
        verifyNoMoreInteractions(toMemory)
        verify(fromStorage, only()).invoke(eq(params))
        verify(toStorage, only()).invoke(eq(params), eq(domain))
    }

    @Test
    fun `fromNetwork should be invoked on IO`() = fastTest {
        memCache[params] = cachedDomain

        whenever(fromNetwork.invoke(eq(params))).thenReturn(domain)

        dataFlowDelegate.observe(params = params, forceReload = true).test(backgroundScope)

        verifyNoMoreInteractions(fromNetwork)

        ioScheduler.triggerActions()

        verify(fromNetwork).invoke(any())
    }

    @Test
    fun `WHEN unsubscribed from network reload THEN data is still saved to storage and memory`() = fastTest {
        val delayedNetwork = suspend {
            delay(1.seconds)
            domain
        }
        fromNetwork.stub {
            onBlocking { invoke(eq(params)) } doSuspendableAnswer { delayedNetwork() }
        }
        storage[params] = cachedDomain

        val testObserver =
            dataFlowDelegate.observe(params = params, forceReload = false)
                .extractContent()
                .firstOrError() // gets storage and unsubscribes before network emits
                .test(backgroundScope)

        ioScheduler.triggerActions()

        testObserver.assertValueCount(1)
        testObserver.assertValueAt(0, cachedDomain)
        testObserver.assertComplete()

        //storage domain moved to memory
        verify(toMemory).invoke(eq(params), eq(cachedDomain))
        //network domain not moved to memory yet
        verify(toMemory, never()).invoke(eq(params), eq(domain))

        computationScheduler.advanceTimeBy(
            1,
            TimeUnit.SECONDS
        ) //network finishes after observer unsubscribed
        advanceTimeBy(delayTimeMillis = 2_000)  // 1000 is not enough

        //network domain moved to memory
        verify(toMemory, atLeastOnce()).invoke(eq(params), eq(domain))
        //network domain moved to storage
        verify(toStorage, atLeastOnce()).invoke(eq(params), eq(domain))
    }

    @Test
    fun `WHEN unsubscribed from dfd AND dod is globally cleared THEN data is not saved`() = fastTest {
        DodGlobal.networkTimeoutSeconds = 60L

        val delayedNetwork = suspend {
            delay(10.seconds)
            domain
        }
        fromNetwork.stub {
            onBlocking { invoke(eq(params)) } doSuspendableAnswer { delayedNetwork() }
        }
        storage[params] = cachedDomain

        dataFlowDelegate.observe(params = params, forceReload = false)
            .extractContent()
            .firstOrError() // gets storage and un-subscribes before network emits
            .test(backgroundScope)
        ioScheduler.triggerActions()
        runCurrent()

        //network is not finished yet and we dispose DOD globally
        computationScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        advanceTimeBy(delayTimeMillis = 6_000)
        DodGlobal.clearPendingNetwork()
        computationScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        advanceTimeBy(delayTimeMillis = 5_000)

        //network result is not stored
        verify(toMemory, never()).invoke(eq(params), eq(domain))
        verify(toStorage, never()).invoke(eq(params), eq(domain))
    }

    @Test
    fun `WHEN unsubscribed from network and data NOT arrives in 60 seconds THEN data not saved`() = fastTest {
        DodGlobal.networkTimeoutSeconds = 60L

        val delayedNetwork = suspend {
            delay(2.minutes)
            domain
        }
        fromNetwork.stub {
            onBlocking { invoke(eq(params)) } doSuspendableAnswer { delayedNetwork() }
        }
        storage[params] = cachedDomain

        val testObserver =
            dataFlowDelegate.observe(params = params, forceReload = false)
                .extractContent()
                .firstOrError() // gets storage and un-subscribes before network emits
                .test(backgroundScope)

        ioScheduler.triggerActions()

        testObserver.assertValueCount(1)
        testObserver.assertValueAt(0, cachedDomain)
        testObserver.assertComplete()

        //storage domain moved to memory
        verify(toMemory).invoke(eq(params), eq(cachedDomain))
        //network domain not moved to memory yet
        verify(toMemory, never()).invoke(eq(params), eq(domain))

        //network reload terminates after 60 seconds
        ioScheduler.advanceTimeBy(60, TimeUnit.SECONDS)
        advanceTimeBy(delayTimeMillis = 61_000)

        //network domain not moved to memory
        verify(toMemory, never()).invoke(eq(params), eq(domain))
        //network domain not moved to storage
        verify(toStorage, never()).invoke(eq(params), eq(domain))


        //re-subscription will re-trigger network after 60 seconds timeout
        whenever(fromNetwork.invoke(eq(params))).thenReturn(domain2)
        dataFlowDelegate.observe(params = params, forceReload = false)
            .test(backgroundScope)

        ioScheduler.triggerActions()

        //network domain2 moved to memory
        verify(toMemory, atLeastOnce()).invoke(eq(params), eq(domain2))
        //network domain2 moved to storage
        verify(toStorage, atLeastOnce()).invoke(eq(params), eq(domain2))
    }

    @Test
    fun `WHEN unsubscribed from network stream AND error occurs THEN next observe will reload`() = fastTest {
        val delayedNetwork = suspend {
            delay(1.seconds)
            throw backendException
        }
        fromNetwork.stub {
            onBlocking { invoke(eq(params)) } doSuspendableAnswer { delayedNetwork() }
        }
        storage[params] = cachedDomain

        val testObserver =
            dataFlowDelegate.observe(params = params, forceReload = false)
                .extractContent()
                .firstOrError() // gets storage and unsubscribes before network emits
                .test(backgroundScope)

        ioScheduler.triggerActions()
        advanceTimeBy(delayTimeMillis = 2_000)

        verify(toMemory).invoke(eq(params), eq(cachedDomain)) //storage domain moved to memory

        testObserver.assertValueCount(1)
        testObserver.assertValueAt(0, cachedDomain)
        testObserver.assertComplete()

        ioScheduler.advanceTimeBy(
            1,
            TimeUnit.SECONDS
        ) //network finishes with error after observer unsubscribed
        advanceTimeBy(delayTimeMillis = 1_000)

        verifyNoMoreInteractions(toMemory)
        verifyNoMoreInteractions(toStorage)

        // can't use `whenever` or `stub` here: if mock was configured to throw exception,
        // attempt to re-configure it throws that exception (bug in Mockito?)
        fromNetwork = SuspendingFrom { domain }

        //observing with forceReload = false expecting to trigger network call again
        dataFlowDelegate.observe(params = params, forceReload = false)
            .extractContent()
            .firstOrError() // gets storage and unsubscribes before network emits
            .test(backgroundScope)

        ioScheduler.triggerActions()

        //network domain moved to memory
        verify(toMemory, atLeastOnce()).invoke(eq(params), eq(domain))
        //network domain moved to storage
        verify(toStorage, atLeastOnce()).invoke(eq(params), eq(domain))
    }

    @Test
    fun `WHEN previous fromNetwork failed THEN observe forceReload=false will reload`() = fastTest {
        whenever(fromNetwork.invoke(eq(params))) doThrow backendException
        storage[params] = cachedDomain

        dataFlowDelegate.observe(params = params, forceReload = false)
            .extractContent()
            .test(backgroundScope)
            .apply { ioScheduler.triggerActions() }
            .assertError(backendException)

        assertEquals(cachedDomain, memCache[params])
        verify(fromNetwork, times(1)).invoke(eq(params))

        // can't use whenever or stub here: if mock was configured to throw exception,
        // attempt to re-configure it throws that exception (bug in Mockito?)
        fromNetwork = SuspendingFrom { domain }

        dataFlowDelegate.observe(params = params, forceReload = false)
            .extractContent()
            .test(backgroundScope)
            .apply { ioScheduler.triggerActions() }
            .assertValues(cachedDomain, domain)

        // we can't validate number of `fromNetwork` calls, but it is implicitly validated by `assertValues`
    }

    @Test
    fun `WHEN previous fromNetwork failed with NoSuchElementException THEN observe forceReload=false will not reload`() = fastTest {
        class CustomException : NoSuchElementException()

        whenever(fromNetwork.invoke(eq(params))) doThrow CustomException()
        storage[params] = cachedDomain

        dataFlowDelegate.observe(params = params, forceReload = false)
            .extractContent(consumeErrors = { error, _ -> error.takeUnless { it is CustomException } })
            .test(backgroundScope)
            .apply { ioScheduler.triggerActions() }
            .assertValues(cachedDomain)

        assertEquals(cachedDomain, memCache[params])
        verify(fromNetwork, times(1)).invoke(eq(params))

        dataFlowDelegate.observe(params = params, forceReload = false)
            .extractContent()
            .test(backgroundScope)
            .apply { ioScheduler.triggerActions() }
            .assertValues(cachedDomain)

        verifyNoMoreInteractions(fromNetwork)
    }

    @Test
    fun `FORCE observing data when memory cache IS NOT EMPTY`() = fastTest {
        whenever(fromNetwork.invoke(eq(params))).thenReturn(domain)
        memCache[params] = cachedDomain

        val testObserver =
            dataFlowDelegate.observe(params = params, forceReload = true).test(backgroundScope)

        testObserver.assertValueCount(1)
        testObserver.assertValueAt(0, Data(content = cachedDomain, error = null, loading = true))

        ioScheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS)
        advanceUntilIdle()

        testObserver.assertValueCount(2)
        testObserver.assertValueAt(1, Data(content = domain, error = null, loading = false))

        verify(fromNetwork, only()).invoke(eq(params))
        verify(fromMemory, only()).invoke(eq(params))
        verify(toMemory, only()).invoke(eq(params), eq(domain))
        verifyNoMoreInteractions(fromStorage)
        verify(toStorage, only()).invoke(eq(params), eq(domain))
    }

    @Test
    fun `WHEN fromNetwork is retried AND notifyFromMemory called THEN subscriber receives the value`() = fastTest {
        fromNetwork = SuspendingFrom {
            delay(Duration.INFINITE)
            domain
        }
        memCache[params] = cachedDomain

        val testObserver =
            dataFlowDelegate.observe(params = params, forceReload = true).test(backgroundScope)

        testObserver.assertValueCount(1)
        testObserver.assertValueAt(0, Data(content = cachedDomain, error = null, loading = true))

        val err = IllegalStateException("From Network is Retried Outside")
        dataFlowDelegate.notifyFromMemory(error = err, loading = true) { it == params }

        testObserver.assertValueAt(1, Data(content = cachedDomain, error = err, loading = true))
    }

    @Test
    fun `FORCE observing data when memory cache IS EMPTY and storage IS EMPTY and server returns ERROR`() = fastTest {
        whenever(fromNetwork.invoke(eq(params))) doThrow backendException

        val testObserver =
            dataFlowDelegate.observe(params = params, forceReload = true).test(backgroundScope)

        testObserver.assertValueCount(1)
        testObserver.assertValueAt(0, Data(null, error = null, loading = true))

        ioScheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS)

        testObserver.assertValueCount(2)
        testObserver.assertValueAt(1, Data(null, error = backendException, loading = false))


        verify(fromNetwork, only()).invoke(eq(params))
        verify(fromMemory, times(2)).invoke(eq(params))
        verifyNoMoreInteractions(toMemory)
        verify(fromStorage, only()).invoke(eq(params))
        verifyNoMoreInteractions(toStorage)
    }

    @Test
    fun `FORCE observing data when memory cache IS EMPTY and storage IS NOT EMPTY and server returns ERROR`() = fastTest {
        whenever(fromNetwork.invoke(eq(params))) doThrow backendException
        storage[params] = cachedDomain

        val testObserver =
            dataFlowDelegate.observe(params = params, forceReload = true).test(backgroundScope)
        testObserver.assertValueCount(1)
        testObserver.assertValueAt(0, Data(null, error = null, loading = true))

        ioScheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS)

        testObserver.assertValueCount(3)

        testObserver.assertValueAt(1, Data(cachedDomain, error = null, loading = true))
        testObserver.assertValueAt(2, Data(cachedDomain, error = backendException, loading = false))

        verify(fromNetwork, only()).invoke(eq(params))
        verify(fromMemory, times(2)).invoke(eq(params))
        verify(toMemory, only()).invoke(eq(params), eq(cachedDomain))
        verify(fromStorage, only()).invoke(eq(params))
        verifyNoMoreInteractions(toStorage)
    }

    @Test
    fun `FORCE observing data when memory cache IS NOT EMPTY and server returns ERROR`() = fastTest {
        whenever(fromNetwork.invoke(eq(params))) doThrow backendException
        memCache[params] = cachedDomain

        val testObserver =
            dataFlowDelegate.observe(params = params, forceReload = true).test(backgroundScope)

        testObserver.assertValueCount(1)
        testObserver.assertValueAt(0, Data(content = cachedDomain, error = null, loading = true))

        ioScheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS)

        testObserver.assertValueCount(2)
        testObserver.assertValueAt(
            1,
            Data(content = cachedDomain, error = backendException, loading = false)
        )

        verify(fromNetwork, only()).invoke(eq(params))
        verify(fromMemory, times(2)).invoke(eq(params))
        verifyNoMoreInteractions(toMemory)
        verifyNoMoreInteractions(fromStorage)
        verifyNoMoreInteractions(toStorage)
    }

    @Test
    fun `NOT FORCE observing data when memory cache IS EMPTY and storage IS EMPTY`() = fastTest {
        whenever(fromNetwork.invoke(eq(params))).thenReturn(domain)

        val testObserver =
            dataFlowDelegate.observe(params = params, forceReload = false).test(backgroundScope)
        testObserver.assertValueCount(1)
        testObserver.assertValueAt(0, Data(null, error = null, loading = true))

        ioScheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS)

        testObserver.assertValueCount(2)
        testObserver.assertValueAt(1, Data(domain, error = null, loading = false))

        verify(fromNetwork, only()).invoke(eq(params))
        verify(fromMemory, only()).invoke(eq(params))
        verify(toMemory, only()).invoke(eq(params), eq(domain))
        verify(fromStorage, only()).invoke(eq(params))
        verify(toStorage, only()).invoke(eq(params), eq(domain))
    }

    @Test
    fun `NOT FORCE observing data when memory cache IS EMPTY and storage IS NOT EMPTY`() = fastTest {
        whenever(fromNetwork.invoke(eq(params))).thenReturn(domain)
        storage[params] = cachedDomain

        val testObserver =
            dataFlowDelegate.observe(params = params, forceReload = false).test(backgroundScope)
        testObserver.assertValueCount(1)
        testObserver.assertValueAt(0, Data(null, error = null, loading = true))

        ioScheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS)

        testObserver.assertValueCount(3)
        testObserver.assertValueAt(1, Data(cachedDomain, error = null, loading = true))
        testObserver.assertValueAt(2, Data(domain, error = null, loading = false))

        verify(fromNetwork, only()).invoke(eq(params))
        verify(fromMemory, only()).invoke(eq(params))
        verify(toMemory).invoke(eq(params), eq(cachedDomain))
        verify(toMemory).invoke(eq(params), eq(domain))
        verifyNoMoreInteractions(toMemory)
        verify(fromStorage, only()).invoke(eq(params))
        verify(toStorage).invoke(eq(params), eq(domain))
    }

    @Test
    fun `NOT FORCE observing data when memory cache IS NOT EMPTY`() = fastTest {
        memCache[params] = cachedDomain

        val testObserver =
            dataFlowDelegate.observe(params = params, forceReload = false).test(backgroundScope)

        testObserver.assertValueCount(1)
        testObserver.assertValueAt(0, Data(content = cachedDomain, error = null, loading = false))

        verifyNoMoreInteractions(fromNetwork)
        verify(fromMemory, only()).invoke(eq(params))
        verifyNoMoreInteractions(toMemory)
        verifyNoMoreInteractions(fromStorage)
        verifyNoMoreInteractions(toStorage)
    }

    @Test
    fun `NOT FORCE observing data when memory cache IS EMPTY and storage IS EMPTY and server returns ERROR`() = fastTest {
        whenever(fromNetwork.invoke(eq(params))) doThrow backendException

        val testObserver =
            dataFlowDelegate.observe(params = params, forceReload = false).test(backgroundScope)

        testObserver.assertValueCount(1)
        testObserver.assertValueAt(0, Data(null, error = null, loading = true))

        ioScheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS)

        testObserver.assertValueCount(2)
        testObserver.assertValueAt(1, Data(null, error = backendException, loading = false))

        verify(fromNetwork, only()).invoke(eq(params))
        verify(fromMemory, times(2)).invoke(eq(params))
        verifyNoMoreInteractions(toMemory)
        verify(fromStorage, only()).invoke(eq(params))
        verifyNoMoreInteractions(toStorage)
    }

    @Test
    fun `NOT FORCE observing data when memory cache IS EMPTY and storage IS NOT EMPTY and server returns ERROR`() = fastTest {
        whenever(fromNetwork.invoke(eq(params))) doThrow backendException
        storage[params] = cachedDomain

        val testObserver =
            dataFlowDelegate.observe(params = params, forceReload = false).test(backgroundScope)

        testObserver.assertValueCount(1)
        testObserver.assertValueAt(0, Data(null, error = null, loading = true))

        ioScheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS)

        testObserver.assertValueCount(3)
        testObserver.assertValueAt(1, Data(cachedDomain, error = null, loading = true))

        testObserver.assertValueAt(2, Data(cachedDomain, error = backendException, loading = false))

        verify(fromNetwork, only()).invoke(eq(params))
        verify(fromMemory, times(2)).invoke(eq(params))
        verify(toMemory, only()).invoke(eq(params), eq(cachedDomain))
        verify(fromStorage, only()).invoke(eq(params))
        verifyNoMoreInteractions(toStorage)
    }

    @Test
    fun `observing data for MORE THAN ONE observer`() = fastTest {
        var firstTime = true

        fromNetwork = SuspendingFrom {
            if (firstTime) {
                firstTime = false
                domain
            } else {
                throw backendException
            }
        }
        memCache[params] = cachedDomain

        val testObserver1 =
            dataFlowDelegate.observe(params = params, forceReload = false).test(backgroundScope)

        testObserver1.assertValueCount(1)
        testObserver1.assertValueAt(0, Data(content = cachedDomain, error = null, loading = false))

        // refresh with result
        val testObserver2 =
            dataFlowDelegate.observe(params = params, forceReload = true).test(backgroundScope)

        testObserver1.assertValueCount(2)
        testObserver1.assertValueAt(0, Data(content = cachedDomain, error = null, loading = false))

        testObserver2.assertValueCount(1)
        testObserver2.assertValueAt(0, Data(content = cachedDomain, error = null, loading = true))

        ioScheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS)

        testObserver1.assertValueCount(3)
        testObserver1.assertValueAt(2, Data(content = domain, error = null, loading = false))

        testObserver2.assertValueCount(2)
        testObserver2.assertValueAt(1, Data(content = domain, error = null, loading = false))

        verify(toMemory).invoke(eq(params), eq(domain))

        //refresh with error
        val testObserver3 =
            dataFlowDelegate.observe(params = params, forceReload = true).test(backgroundScope)

        testObserver1.assertValueCount(4)
        testObserver1.assertValueAt(3, Data(content = domain, error = null, loading = true))

        testObserver2.assertValueCount(3)
        testObserver2.assertValueAt(2, Data(content = domain, error = null, loading = true))

        testObserver3.assertValueCount(1)
        testObserver3.assertValueAt(0, Data(content = domain, error = null, loading = true))

        ioScheduler.advanceTimeBy(10, TimeUnit.MILLISECONDS)

        testObserver1.assertValueCount(5)
        testObserver1.assertValueAt(4, Data(content = domain, error = backendException, loading = false))

        testObserver2.assertValueCount(4)
        testObserver2.assertValueAt(3, Data(content = domain, error = backendException, loading = false))

        testObserver3.assertValueCount(2)
        testObserver3.assertValueAt(1, Data(content = domain, error = backendException, loading = false))
    }

    @Test
    fun `re-subscribing to constructed stream re-fetches memory cache`() = fastTest {
        val flow = dataFlowDelegate.observe(params, forceReload = true)

        flow.take(1).test(backgroundScope)
        verify(fromMemory, times(1)).invoke(eq(params))

        flow.take(1).test(backgroundScope)

        verify(fromMemory, times(2)).invoke(eq(params))
    }

    @Test
    fun `reload completable notifies subscribers and updates storage and memory`() = fastTest {
        // given
        whenever(fromNetwork.invoke(eq(params))).thenReturn(domain)

        val testObserver =
            dataFlowDelegate.observe(params = params, forceReload = false).test(backgroundScope)

        ioScheduler.triggerActions()

        testObserver.assertValues(
            Data(null, null, true),
            Data(domain, null, false)
        )

        val updatedDomain: Domain = "updated_domain"
        whenever(fromNetwork.invoke(eq(params))).thenReturn(updatedDomain)

        // when
        dataFlowDelegate.reload(params = params)

        // then
        ioScheduler.triggerActions() // network executes

        testObserver.assertValueCount(4)
        testObserver.assertValueAt(2, Data(domain, null, true))
        testObserver.assertValueAt(3, Data(updatedDomain, null, false))

        verify(toMemory).invoke(params, updatedDomain)
        verify(toStorage).invoke(params, updatedDomain)
    }

    @Test
    fun `reload await completes only after network request is completed`() = fastTest {
        // given
        whenever(fromNetwork.invoke(eq(params))).thenReturn(domain)

        // when
        val reload = backgroundScope.launch {
            dataFlowDelegate.reload(params = params, await = true)
        }

        // then
        reload.assertNotComplete()  // not complete before network executes
        ioScheduler.triggerActions()
        reload.assertComplete() // not complete after network executed
    }

    @Test
    fun `reload await emits error if network fails`() {
        assertThrows(IllegalStateException::class.java) {
            fastTest {
                // given
                whenever(fromNetwork.invoke(eq(params))).thenReturn(domain)

                val error = IllegalStateException()
                whenever(fromNetwork.invoke(eq(params))) doThrow error

                // when
                val reload = backgroundScope.launch {
                    dataFlowDelegate.reload(params = params, await = true)
                }

                // then
                reload.assertNotComplete()
                ioScheduler.triggerActions() // network executes
            }
        }
    }

    @Test
    fun `WHEN storage returns error THEN network data is emitted later`() = fastTest {
        val error = IllegalStateException()
        whenever(fromNetwork.invoke(eq(params))).thenReturn(domain)
        whenever(fromStorage.invoke(any())).thenThrow(error)

        val testObserver =
            dataFlowDelegate.observe(params = params, forceReload = false).test(backgroundScope)

        ioScheduler.triggerActions()

        testObserver.assertValueCount(3)
        testObserver.assertValueAt(0, Data(content = null, error = null, loading = true))
        testObserver.assertValueAt(1, Data(content = null, error = error, loading = true))
        testObserver.assertValueAt(2, Data(content = domain, error = null, loading = false))
    }

    @Test
    fun `WHEN memoryIsEmpty and dfd is observed multiple times THEN fromStorage is called once`() = fastTest {
        val counter = AtomicInteger(0)

        whenever(fromNetwork.invoke(eq(params))).thenReturn(domain)
        whenever(fromStorage.invoke(eq(params))).thenAnswer {
            counter.incrementAndGet()
            cachedDomain
        }

        dataFlowDelegate.observe(params = params, forceReload = true).test(backgroundScope)
        dataFlowDelegate.observe(params = params, forceReload = true).test(backgroundScope)
        dataFlowDelegate.observe(params = params, forceReload = true).test(backgroundScope)

        ioScheduler.triggerActions()

        assertEquals(1, counter.get())
    }

    @Test
    fun `WHEN forceReload dfd switchMaps to the same forceReload dfd THEN emissions are muted after 2nd iteration`() = fastTest {
        whenever(fromNetwork.invoke(eq(params))).thenReturn(cachedDomain)
        storage[params] = cachedDomain
        memCache.remove(params)

        dataFlowDelegate.observe(params = params, forceReload = true).take(100)
            .flatMapLatest {
                dataFlowDelegate.observe(params = params, forceReload = true).take(100)
            }
            .test(backgroundScope)
            .apply {
                ioScheduler.triggerActions()
            }
            // the result does not match RxJava version, due to differences of flatMapLatest vs switchMap
            .assertValueCount(7)
    }

    @Test
    fun `WHEN dfd switchMaps to the same forceReload dfd THEN emissions are muted after 2nd iteration`() = fastTest {
        whenever(fromNetwork.invoke(eq(params))).thenReturn(cachedDomain)
        storage[params] = cachedDomain
        memCache.remove(params)

        dataFlowDelegate.observe(params = params).take(100)
            .flatMapLatest {
                dataFlowDelegate.observe(params = params, forceReload = true).take(100)
            }
            .test(backgroundScope)
            .apply {
                ioScheduler.triggerActions()
            }
            .assertValueCount(7)
    }

    @Test
    fun `WHEN dfd switchMaps to the same forceReload dfd AND fromNetwork returns errors THEN emissions are muted after 2nd iteration`() =
        fastTest {
            whenever(fromNetwork.invoke(eq(params))) doThrow backendException
            storage[params] = cachedDomain
            memCache.remove(params)

            dataFlowDelegate.observe(params = params).take(100)
                .flatMapLatest {
                    dataFlowDelegate.observe(params = params, forceReload = true).take(100)
                }
                .test(backgroundScope)
                .apply {
                    ioScheduler.triggerActions()
                }
                .assertValueCount(7)
        }

    @Test
    fun `WHEN fromNetwork failing with delay AND notifyFromMemory called in between THEN subscriber receives latest value`() = fastTest {
        val delayedNetwork = suspend {
            delay(1.seconds)
            throw backendException
        }
        fromNetwork.stub {
            onBlocking { invoke(eq(params)) } doSuspendableAnswer { delayedNetwork() }
        }
        memCache[params] = cachedDomain

        val testObserver =
            dataFlowDelegate.observe(params = params, forceReload = true).test(backgroundScope)

        testObserver.assertValueCount(1)
        testObserver.assertValueAt(0, Data(content = cachedDomain, error = null, loading = true))

        memCache[params] = domain2
        dataFlowDelegate.notifyFromMemory { it == params }

        testObserver.assertValueCount(2)
        testObserver.assertValueAt(1, Data(content = domain2))

        ioScheduler.triggerActions()
        advanceTimeBy(2_000)
        ioScheduler.triggerActions()

        testObserver.assertValueCount(3)
        testObserver.assertValueAt(2, Data(content = domain2, error = backendException))
    }

    @Test
    fun `WHEN fromNetwork failing with delay AND updateAll called in between THEN subscriber receives latest value`() = fastTest {
        val delayedNetwork = suspend {
            delay(1.seconds)
            throw backendException
        }
        fromNetwork.stub {
            onBlocking { invoke(eq(params)) } doSuspendableAnswer { delayedNetwork() }
        }
        memCache[params] = cachedDomain

        val testObserver =
            dataFlowDelegate.observe(params = params, forceReload = true).test(backgroundScope)

        testObserver.assertValueCount(1)
        testObserver.assertValueAt(0, Data(content = cachedDomain, error = null, loading = true))

        dataFlowDelegate.updateAll(params, domain2)

        testObserver.assertValueAt(1, Data(content = domain2))

        ioScheduler.triggerActions()
        advanceTimeBy(2_000)
        ioScheduler.triggerActions()

        testObserver.assertValueAt(2, Data(content = domain2, error = backendException))
    }

    @Test
    fun `WHEN fromNetwork failing with delay AND updateMemory called in between THEN subscriber receives latest value`() = fastTest {
        val delayedNetwork = suspend {
            delay(1.seconds)
            throw backendException
        }
        fromNetwork.stub {
            onBlocking { invoke(eq(params)) } doSuspendableAnswer { delayedNetwork() }
        }
        memCache[params] = cachedDomain

        val testObserver =
            dataFlowDelegate.observe(params = params, forceReload = true).test(backgroundScope)

        testObserver.assertValueCount(1)
        testObserver.assertValueAt(0, Data(content = cachedDomain, error = null, loading = true))

        dataFlowDelegate.updateMemory(params, domain2)

        testObserver.assertValueAt(1, Data(content = domain2))

        ioScheduler.triggerActions()
        advanceTimeBy(2_000)
        ioScheduler.triggerActions()

        testObserver.assertValueAt(2, Data(content = domain2, error = backendException))
    }

    @Test
    fun `WHEN fromMemory returns null, fromStorage has value AND fromNetwork fails THEN subscriber receives storage value`() = fastTest {
        val delayedNetwork = suspend {
            delay(1.seconds)
            throw backendException
        }
        fromNetwork.stub {
            onBlocking { invoke(eq(params)) } doSuspendableAnswer { delayedNetwork() }
        }
        whenever(fromMemory.invoke(any())).thenReturn(null)
        storage[params] = cachedDomain

        val testObserver =
            dataFlowDelegate.observe(params = params, forceReload = true).test(backgroundScope)

        testObserver.assertValueCount(1)
        testObserver.assertValueAt(0, Data(content = null, error = null, loading = true))
        ioScheduler.triggerActions()

        testObserver.assertValueCount(2)
        testObserver.assertValueAt(1, Data(content = cachedDomain, error = null, loading = true))

        ioScheduler.triggerActions()
        advanceTimeBy(2_000)
        ioScheduler.triggerActions()

        testObserver.assertValueCount(3)
        testObserver.assertValueAt(2, Data(content = cachedDomain, error = backendException))
    }
}