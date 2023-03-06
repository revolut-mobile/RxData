package com.revolut.rxdata.dfd_wrapper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opentest4j.AssertionFailedError
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

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
class TestHelpersTest {

    private val error = RuntimeException("dummy")
    private val testDispatcher = UnconfinedTestDispatcher()

    private fun fastTest(testBody: suspend TestScope.() -> Unit) = runTest(
        context = testDispatcher,
        dispatchTimeoutMs = 1_000,
        testBody = testBody,
    )

    @Test
    fun `flow base scenario`() = fastTest {
        val flow = flowOf(1, 2, 3)
        val values = flow.toList()
        values shouldBe listOf(1, 2, 3)
    }

    @Test
    fun `firstOrError success`() = fastTest {
        val flow = flowOf(1, 2, 3)
            .firstOrError()
        val values = flow.toList()
        values shouldBe listOf(1)
    }

    @Test
    fun `firstOrError success on hot flow`() = fastTest {
        val flow = MutableStateFlow(1)
            .firstOrError()
        val values = flow.toList()
        values shouldBe listOf(1)
    }

    @Test
    fun `firstOrError fail on empty`() = fastTest {
        assertThrows<NoSuchElementException> {
            val flow = flowOf<Int>()
                .firstOrError()
            flow.toList()
        }
    }

    @Test
    fun `assertTerminated success on complete`() = fastTest {
        val testObserver = flowOf(1)
            .test(backgroundScope)
        testObserver.assertTerminated()
    }

    @Test
    fun `assertTerminated success on error`() = fastTest {
        val testObserver = flow<Unit> { throw error }
            .test(backgroundScope)
        testObserver.assertTerminated()
    }

    @Test
    fun `assertComplete success`() = fastTest {
        val testObserver = flowOf(1)
            .test(backgroundScope)
        testObserver.assertComplete()
    }

    @Test
    fun `assertComplete fail on incomplete`() = fastTest {
        val testObserver = MutableStateFlow(1)
            .test(backgroundScope)
        assertThrows<AssertionFailedError> {
            testObserver.assertComplete()
        }
    }

    @Test
    fun `assertComplete fail on error`() = fastTest {
        val testObserver = flow<Unit> { throw error }
            .test(backgroundScope)
        assertThrows<AssertionFailedError> {
            testObserver.assertComplete()
        }
    }

    @Test
    fun `assertError success`() = fastTest {
        val testObserver = flow<Unit> { throw error }
            .test(backgroundScope)
        testObserver.assertError(error)
    }

    @Test
    fun `assertError fail`() = fastTest {
        val testObserver = flowOf(1)
            .test(backgroundScope)
        assertThrows<AssertionFailedError> {
            testObserver.assertError(error)
        }
    }

    @Test
    fun `assertError fail on different exception`() = fastTest {
        val testObserver = flow<Unit> { throw error }
            .test(backgroundScope)
        assertThrows<AssertionFailedError> {
            testObserver.assertError(RuntimeException("another"))
        }
    }

    @Test
    fun `awaitTerminalEvent return true on complete`() = fastTest {
        val testObserver = flowOf(1)
            .test(backgroundScope)
        testObserver.awaitTerminalEvent(1.seconds) shouldBe true
    }

    @Test
    fun `awaitTerminalEvent return true on error`() = fastTest {
        val testObserver = flow<Unit> { throw error }
            .test(backgroundScope)
        testObserver.awaitTerminalEvent(1.seconds) shouldBe true
    }

    @Test
    fun `awaitTerminalEvent return true after waiting`() = fastTest {
        val testObserver = flow<Unit> { delay(500.milliseconds) }
            .test(backgroundScope)
        testObserver.awaitTerminalEvent(1.seconds) shouldBe true
    }

    @Test
    fun `awaitTerminalEvent return false after waiting`() = fastTest {
        val testObserver = flow<Unit> { delay(2.seconds) }
            .test(backgroundScope)
        testObserver.awaitTerminalEvent(1.seconds) shouldBe false
    }

    @Test
    fun `awaitTerminalEvent return false on hot flow`() = fastTest {
        val testObserver = MutableStateFlow(1)
            .test(backgroundScope)
        testObserver.awaitTerminalEvent(1.seconds) shouldBe false
    }

    @Test
    fun `awaitTerminalEvent cancels collection`() = runTest(dispatchTimeoutMs = 200) {
        withContext(Dispatchers.IO) {
            val testObserver = flow<Unit> { delay(2.seconds) }
                .test(this)
            testObserver.awaitTerminalEvent(100.milliseconds)
        }
    }
}