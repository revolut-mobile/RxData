package com.revolut.flowdata.extensions

import app.cash.turbine.test
import com.revolut.data.model.Data
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
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
            Assertions.assertEquals("B", expectMostRecentItem())
        }
    }

    @Test
    fun `Loading items with errors are skipped`() = runTest {
        val error = IllegalStateException()

        flowOf(
            Data("A", error, loading = true),
            Data("B", null, loading = false)
        ).filterWhileLoading().extractContent().test {
            Assertions.assertEquals("B", expectMostRecentItem())
        }
    }

    @Test
    fun `Error after loading`() = runTest {
        val error = IllegalStateException()

        flowOf(
            Data("A", null, loading = true),
            Data("A", error, loading = false)
        ).filterWhileLoading().extractContent().test {
            Assertions.assertEquals(error, awaitError())
        }
    }


    @Test
    fun `Error without content is extracted and terminates the stream`() = runTest {
        val error = IllegalStateException()

        flowOf(
            Data("A", null, loading = true),
            Data(null, error, loading = false)
        ).filterWhileLoading().extractContent().test {
            Assertions.assertEquals(error, awaitError())
        }
    }

    //endregion
}