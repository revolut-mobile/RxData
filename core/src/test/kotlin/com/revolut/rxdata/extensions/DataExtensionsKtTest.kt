package com.revolut.rxdata.extensions

import com.nhaarman.mockito_kotlin.mock
import com.revolut.rxdata.core.Data
import com.revolut.rxdata.core.extensions.isEmpty
import com.revolut.rxdata.core.extensions.isNotEmpty
import com.revolut.rxdata.core.extensions.mapData
import com.revolut.rxdata.core.extensions.mapDataErrorToContent
import org.junit.Assert.*
import org.junit.Test

/*
 * Copyright (C) 2020 Revolut
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

class DataExtensionsKtTest {

    @Test
    fun `isEmpty should return true when there is no content in Data`() {
        assertTrue(Data(content = null, error = null, loading = false).isEmpty())
    }

    @Test
    fun `isEmpty should return false when Data has content`() {
        assertFalse(Data(content = Unit, error = null, loading = false).isEmpty())
    }

    @Test
    fun `isNotEmpty should return false when there is no content in Data`() {
        assertFalse(Data(content = null, error = null, loading = false).isNotEmpty())
    }

    @Test
    fun `isNotEmpty should return true when Data has content`() {
        assertTrue(Data(content = Unit, error = null, loading = false).isNotEmpty())
    }

    @Test
    fun `mapData should change the content`() {
        val data = Data(content = "content", error = null, loading = false)
        val expected = Data(content = "changed content", error = null, loading = false)
        assertEquals(expected, data.mapData { content -> "changed $content" })
    }

    @Test
    fun `mapData should change the error to an exception thrown by the modifying function`() {
        val data = Data(content = Unit, error = null, loading = false)
        val exception: Exception = mock()
        val expected = Data(content = null, error = exception, loading = false)
        assertEquals(expected, data.mapData { throw exception })
    }

    @Test
    fun `mapDataErrorToContent should return the same data if there is no error`() {
        val data = Data(content = "content", error = null, loading = false)
        assertEquals(data, data.mapDataErrorToContent { "changed content" })
    }

    @Test
    fun `mapDataErrorToContent should change the content`() {
        val data = Data(content = null, error = mock(), loading = false)
        val expected = Data(content = Unit, error = null, loading = false)
        assertEquals(expected, data.mapDataErrorToContent { Unit })
    }

    @Test
    fun `mapDataErrorToContent should change the error to an exception thrown by the modifying function`() {
        val data = Data(content = null, error = Exception("original"), loading = false)
        val modifiedException = Exception("modified")
        val expected = Data(content = null, error = modifiedException, loading = false)
        assertEquals(expected, data.mapDataErrorToContent { throw modifiedException })
    }
}
