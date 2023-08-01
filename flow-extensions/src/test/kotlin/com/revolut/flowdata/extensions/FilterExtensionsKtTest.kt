package com.revolut.flowdata.extensions

import app.cash.turbine.test
import com.revolut.data.model.Data
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
class FilterExtensionsKtTest {

    @Test
    fun `GIVEN condition does not match WHEN filterContent THEN no values`() = runTest {
        flowOf(
            Data(content = emptyList<Unit>()),
        ).filterContent { it.isNotEmpty() }
            .test {
                awaitComplete()
            }
    }

    @Test
    fun `GIVEN condition matches WHEN filterContent THEN assert value`() = runTest {
        flowOf(
            Data(content = listOf(Unit)),
        ).filterContent { it.isNotEmpty() }
            .test {
                assertEquals(Data(listOf(Unit)), awaitItem())
                awaitComplete()
            }
    }

    @Test
    fun `GIVEN loading data WHEN filterContent THEN assert value`() = runTest {
        flowOf(
            Data<List<Unit>>(loading = true),
        ).filterContent { it.isNotEmpty() }
            .test {
                assertEquals(Data<List<Unit>>(loading = true), awaitItem())
                awaitComplete()
            }
    }

    @Test
    fun `GIVEN error data WHEN filterContent THEN assert value`() = runTest {
        flowOf(
            Data<List<Unit>>(error = RuntimeException()),
        ).filterContent { it.isNotEmpty() }
            .test {
                assertEquals(Data<List<Unit>>(error = RuntimeException()), awaitItem())
                awaitComplete()
            }
    }
}