package com.revolut.flowdata.extensions

import app.cash.turbine.test
import com.revolut.data.model.Data
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
class LoadingExtensionsKtTest {

    //region filterWhileLoading

    @Test
    fun `Loading items are skipped`() = runTest {
        flowOf(
            Data("A", null, loading = true),
            Data("B", null, loading = false)
        ).filterWhileLoading().extractContent().test {
            assertEquals("B", expectMostRecentItem())
        }
    }

    @Test
    fun `Loading items with errors are skipped`() = runTest {
        val error = IllegalStateException()

        flowOf(
            Data("A", error, loading = true),
            Data("B", null, loading = false)
        ).filterWhileLoading().extractContent().test {
            assertEquals("B", expectMostRecentItem())
        }
    }

    @Test
    fun `Error after loading`() = runTest {
        val error = IllegalStateException()

        flowOf(
            Data("A", null, loading = true),
            Data("A", error, loading = false)
        ).filterWhileLoading().extractContent().test {
            assertEquals(error, awaitError())
        }
    }


    @Test
    fun `Error without content is extracted and terminates the stream`() = runTest {
        val error = IllegalStateException()

        flowOf(
            Data("A", null, loading = true),
            Data(null, error, loading = false)
        ).filterWhileLoading().extractContent().test {
            assertEquals(error, awaitError())
        }
    }

    //endregion

    //region takeUntilLoaded

    @Test
    fun `Loading item and loaded item are returned, and loaded item terminates the stream`() =
        runTest {
            flowOf(
                Data("A", null, loading = true),
                Data("B", null, loading = false),
                Data("C", null, loading = true),
            ).takeUntilLoaded().test {
                assertEquals(Data("A", null, loading = true), awaitItem())
                assertEquals(Data("B", null, loading = false), awaitItem())
                awaitComplete()
                expectNoEvents()
            }
        }

    @Test
    fun `Loaded item, which is the first and the only item in the stream, is returned and terminates the stream`() =
        runTest {
            flowOf(
                Data("A", null, loading = false),
            ).takeUntilLoaded().test {
                assertEquals(Data("A", null, loading = false), awaitItem())
                awaitComplete()
                expectNoEvents()
            }
        }

    @Test
    fun `Loading item with error and loaded item are returned, and loaded item terminates the stream`() =
        runTest {
            val error = IllegalStateException()

            flowOf(
                Data("A", error, loading = true),
                Data("B", null, loading = false),
                Data("C", null, loading = true),
            ).takeUntilLoaded().test {
                assertEquals(Data("A", error, loading = true), awaitItem())
                assertEquals(Data("B", null, loading = false), awaitItem())
                awaitComplete()
                expectNoEvents()
            }
        }

    @Test
    fun `Loading item and loaded item with error are returned, and loaded item with error terminates the stream`() =
        runTest {
            val error = IllegalStateException()

            flowOf(
                Data("A", null, loading = true),
                Data("B", error, loading = false),
                Data("C", null, loading = true),
            ).takeUntilLoaded().test {
                assertEquals(Data("A", null, loading = true), awaitItem())
                assertEquals(Data("B", error, loading = false), awaitItem())
                awaitComplete()
                expectNoEvents()
            }
        }

    @Test
    fun `Loaded item with error, which is the first and the only item in the stream, is returned and terminates the stream`() =
        runTest {
            val error = IllegalStateException()

            flowOf(
                Data("A", error, loading = false),
            ).takeUntilLoaded().test {
                assertEquals(Data("A", error, loading = false), awaitItem())
                awaitComplete()
                expectNoEvents()
            }
        }

    //endregion
}