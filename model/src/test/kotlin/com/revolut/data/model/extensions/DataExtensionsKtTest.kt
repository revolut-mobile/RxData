package com.revolut.data.model.extensions

import com.revolut.data.model.Data
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
class DataExtensionsKtTest {

    @Test
    fun `GIVEN content WHEN mapData THEN return new data`() = runTest {
        val data = Data(content = Unit).mapDataSuspended { emptyList<Int>() }
        val expected = Data(content = emptyList<Int>())
        assertEquals(expected, data)
    }

    @Test
    fun `GIVEN content and exception thrown by the modifying function WHEN mapData THEN data contains exception`() = runTest {
        val data = Data(content = Unit, error = null, loading = false)
        val expected = Data(content = null, error = Exception(), loading = false)
        assertEquals(expected, data.mapDataSuspended { throw Exception() })
    }
}