package com.revolut.dfd


import com.revolut.flow_core.core.Data
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList


class DataFlowDelegateTest {

    private val testDispatcher = TestCoroutineDispatcher()

    private val memoryCache = ConcurrentHashMap<String, String>()
    private val storage = ConcurrentHashMap<String, String>()

    @Before
    fun setUp() {
        memoryCache.clear()
        storage.clear()
        DataFlowDelegateDispatchers.setIoDispatcher(testDispatcher)
    }


    @Test
    fun `FORCE observing data when memory cache and db not presented`() = runBlockingTest {
        val param = "param"
        val networkResult = "I'm from network with correct param!"

        val delegate = DataFlowDelegate<String, String>(
            fromNetwork = { p ->
                if (p == param) {
                    networkResult
                } else {
                    error("Unknown param")
                }
            }
        )

        val expectedFirst = Data<String>(loading = true)
        val expectedSecond = Data(content = networkResult)

        val actualValues = delegate.observe(params = param, forceReload = false)
            .collectValues(2)


        assertEquals(expectedFirst, actualValues.first())
        assertEquals(expectedSecond, actualValues.last())
    }

    @Test
    fun `FORCE observing data when  memory cache IS EMPTY and storage IS EMPTY`() =
        runBlockingTest {
            val networkResult = "I'm from network with correct param!"
            val param = "param"
            val delegate = DataFlowDelegate<String, String>(
                fromNetwork = {
                    networkResult
                },
                fromMemory = {
                    null
                },
                fromStorage = {
                    null
                }
            )

            val expectedFirst = Data<String>(loading = true)
            val expectedSecond = Data(content = networkResult)

            val actualValues = delegate.observe(params = param, forceReload = false)
                .collectValues(2)

            assertEquals(expectedFirst, actualValues.first())
            assertEquals(expectedSecond, actualValues.last())
        }

    @Test
    fun `FORCE observing data when  memory cache IS EMPTY and storage throw an exception`() =
        runBlockingTest {
            val networkResult = "I'm from network with correct param!"
            val param = "param"
            val delegate = DataFlowDelegate<String, String>(
                fromNetwork = {
                    networkResult
                },
                fromMemory = {
                    null
                },
                fromStorage = {
                    throw IOException("No data")
                }
            )

            val expectedFirst = Data<String>(loading = true)
            val expectedSecond =
                Data<String>(loading = true, error = IOException("No data"))

            val expectedThird =
                Data<String>(loading = false, content = "I'm from network with correct param!")

            val actualValues = delegate.observe(params = param, forceReload = false)
                .collectValues(3)

            assertEquals(expectedFirst, actualValues.first())
            assertEquals(expectedSecond, actualValues[1])
            assertEquals(expectedThird, actualValues.last())
        }


    @Test
    fun `FORCE observing data when memory cache IS EMPTY and storage IS NOT EMPTY`() =
        runBlockingTest {

            val networkResult = "I'm from network with correct param!"
            val storageResult = "I'm from storage with correct param!"
            val param = "param"
            val delegate = DataFlowDelegate<String, String>(
                fromNetwork = { p ->
                    if (p == param) {
                        networkResult
                    } else {
                        error("Unknown param")
                    }
                },
                fromMemory = {
                    null
                },
                fromStorage = { p ->
                    if (p == param) {
                        storageResult
                    } else {
                        error("Unknown param")
                    }
                }
            )

            val expectedFirst = Data<String>(loading = true)
            val expectedSecond = Data(content = storageResult, loading = true)
            val expectedThird = Data(content = networkResult)

            val actualValues = delegate.observe(params = param, forceReload = true)
                .collectValues(3)


            assertEquals(expectedFirst, actualValues.first())
            assertEquals(expectedSecond, actualValues[1])
            assertEquals(expectedThird, actualValues.last())
        }


    @Test
    fun `FORCE observing data when memory cache IS NOT EMPTY and storage IS NOT EMPTY`() =
        runBlockingTest {
            val networkResult = "I'm from network with correct param!"
            val storageResult = "I'm from storage with correct param!"
            val memCacheResult = "I'm from memCache with correct param!"
            val param = "param"
            val delegate = DataFlowDelegate<String, String>(
                fromNetwork = { p ->
                    if (p == param) {
                        networkResult
                    } else {
                        error("Unknown param")
                    }
                },
                fromMemory = { p ->
                    if (p == param) {
                        memCacheResult
                    } else {
                        error("Unknown param")
                    }
                },
                fromStorage = { p ->
                    if (p == param) {
                        storageResult
                    } else {
                        error("Unknown param")
                    }
                }
            )

            val expectedFirst = Data(loading = true, content = memCacheResult)
            val expectedSecond = Data(content = networkResult)


            val actualValues = delegate.observe(params = param, forceReload = true)
                .collectValues(2)


            assertEquals(expectedFirst, actualValues.first())
            assertEquals(expectedSecond, actualValues.last())
        }


    @Test
    fun `should emit data from memory cache with network error`() = runBlockingTest {
        val storageResult = "I'm from storage with correct param!"
        val memCacheResult = "I'm from memCache with correct param!"
        val param = "param"
        val delegate = DataFlowDelegate<String, String>(
            fromNetwork = { p ->
                if (p == param) {
                    throw IOException("Network error")
                } else {
                    error("Unknown param")
                }
            },
            fromMemory = { p ->
                if (p == param) {
                    memCacheResult
                } else {
                    error("Unknown param")
                }
            },
            fromStorage = { p ->
                if (p == param) {
                    storageResult
                } else {
                    error("Unknown param")
                }
            }
        )

        val expectedFirst = Data(loading = true, content = memCacheResult)
        val expectedSecond =
            Data(content = memCacheResult, error = IOException("Network error"))


        val actualValues = delegate.observe(params = param, forceReload = true)
            .collectValues(2)


        assertEquals(expectedFirst, actualValues.first())
        assertEquals(expectedSecond, actualValues.last())
    }


    @Test
    fun `WHEN update all THEN should trigger toMemory(), toStorage() AND emit new data`() =
        runBlockingTest {
            val networkResult = "I'm from network with correct param!"
            val param = "param"

            val delegate = DataFlowDelegate<String, String>(
                fromNetwork = { p ->
                    if (p == param) {
                        networkResult
                    } else {
                        error("Unknown param")
                    }
                },
                fromMemory = { p ->
                    memoryCache[p]
                },
                fromStorage = { p ->
                    storage[p]
                },
                toStorage = { params, value ->
                    storage[params] = value
                },
                toMemory = { params, value ->
                    memoryCache[params] = value
                }
            )

            val newValue = "I am new value"
            delegate.updateAll(param, newValue)

            val actualValueFromMemory = memoryCache[param]
            val actualValueFromStorage = storage[param]

            assertEquals(newValue, actualValueFromMemory)
            assertEquals(newValue, actualValueFromStorage)

            val actualEmitted = delegate.observe(params = param, forceReload = false)
                .collectValues(1)[0]

            assertEquals(Data(newValue), actualEmitted)
        }

    @Test
    fun `WHEN update memory THEN should trigger toMemory() AND emit new data`() =
        runBlockingTest {
            val networkResult = "I'm from network with correct param!"
            val param = "param"

            val delegate = DataFlowDelegate<String, String>(
                fromNetwork = { p ->
                    if (p == param) {
                        networkResult
                    } else {
                        error("Unknown param")
                    }
                },
                fromMemory = { p ->
                    memoryCache[p]
                },
                fromStorage = { p ->
                    storage[p]
                },
                toStorage = { params, value ->
                    storage[params] = value
                },
                toMemory = { params, value ->
                    memoryCache[params] = value
                }
            )

            val newValue = "I am new value"
            delegate.updateMemory(param, newValue)

            val actualValueFromMemory = memoryCache[param]

            assertEquals(newValue, actualValueFromMemory)

            val actualEmitted = delegate.observe(params = param, forceReload = false)
                .collectValues(1)[0]

            assertEquals(Data(newValue), actualEmitted)
        }

    @Test
    fun `WHEN update storage THEN should trigger toStorage() AND emit new data`() =
        runBlockingTest {
            val networkResult = "I'm from network with correct param!"
            val param = "param"

            val delegate = DataFlowDelegate<String, String>(
                fromNetwork = { p ->
                    if (p == param) {
                        networkResult
                    } else {
                        error("Unknown param")
                    }
                },
                fromMemory = { p ->
                    null
                },
                fromStorage = { p ->
                    storage[p]
                },
                toStorage = { params, value ->
                    storage[params] = value
                },
                toMemory = { params, value ->
                    memoryCache[params] = value
                }
            )

            val newValue = "I am new value"
            delegate.updateStorage(param, newValue)

            val actualValueFromStorage = storage[param]

            assertEquals(newValue, actualValueFromStorage)

            val values = delegate.observe(params = param, forceReload = false)
                .collectValues(3)

            assertEquals(Data<String>(loading = true), values[0])
            assertEquals(Data(content = newValue, loading = true), values[1])
            assertEquals(Data(content = networkResult), values[2])
        }


    private suspend fun <T> Flow<T>.collectValues(count: Int): List<T> {
        val values = CopyOnWriteArrayList<T>()
        take(count)
            .collect(values::add)
        return values
    }
}