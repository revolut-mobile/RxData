package com.revolut.flowdata


import com.revolut.data.model.Data
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList

@ExperimentalCoroutinesApi
class DataFlowDelegateTest {

    private val testDispatcher = TestCoroutineDispatcher()

    @BeforeEach
    fun setUp() {
        DataFlowDelegateDispatchers.setIoDispatcher(testDispatcher)
    }


    @Test
    fun `FORCE observing data when memory cache and db not presented`() = runBlocking {
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
    fun `FORCE observing data when  memory cache IS EMPTY and storage IS EMPTY`() = runBlocking {
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
    fun `FORCE observing data when  memory cache IS EMPTY and storage throw an exception`() = runBlocking {
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
        runBlocking {

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
        runBlocking {
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
    fun `should emit data from memory cache with network error`() = runBlocking {
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


    private suspend fun <T> Flow<T>.collectValues(count: Int): List<T> {
        val values = CopyOnWriteArrayList<T>()
        take(count)
            .collect(values::add)
        return values
    }
}