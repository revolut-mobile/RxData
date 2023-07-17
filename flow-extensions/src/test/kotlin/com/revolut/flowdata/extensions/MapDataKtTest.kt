package com.revolut.flowdata.extensions

import app.cash.turbine.test
import com.revolut.data.model.Data
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

@ExperimentalCoroutinesApi
class MapDataKtTest {

    @Test
    fun `GIVEN content and no error WHEN mapDataErrorToContent THEN should return the same data`() = runTest {
        flowOf(Data(content = "content", error = null, loading = false))
            .mapDataErrorToContent { "changed content" }
            .test {
                assertEquals(Data(content = "content", error = null, loading = false), awaitItem())
                awaitComplete()
            }
    }

    @Test
    fun `GIVEN error data WHEN mapDataErrorToContentTHEN should change the content`() = runTest {
        flowOf(Data(content = null, error = mock(), loading = false))
            .mapDataErrorToContent { Unit }
            .test {
                assertEquals(Data(content = Unit, error = null, loading = false), awaitItem())
                awaitComplete()
            }
    }

    @Test
    fun `GIVEN error data and and modifying function throws exception WHEN mapDataErrorToContent THEN should change the error`() = runTest {
        flowOf(Data(content = null, error = Exception("original"), loading = false))
            .mapDataErrorToContent { throw Exception("modified") }
            .test {
                assertEquals(Data(content = null, error = Exception("modified"), loading = false), awaitItem())
                awaitComplete()
            }
    }

    @Test
    fun `GIVEN content WHEN mapData THEN return new data`() = runTest {
        flowOf(Data(content = Unit))
            .mapDataSuspended { emptyList<Int>() }
            .test {
                assertEquals(Data(content = emptyList<Int>(), error = null, loading = false), awaitItem())
                awaitComplete()
            }
    }

    @Test
    fun `GIVEN content and exception thrown by the modifying function WHEN mapData THEN data contains exception`() = runTest {
        flowOf(Data(content = Unit, error = null, loading = false))
            .mapDataSuspended { throw Exception() }
            .test {
                assertEquals(Data(content = null, error = Exception(), loading = false), awaitItem())
                awaitComplete()
            }
    }
}