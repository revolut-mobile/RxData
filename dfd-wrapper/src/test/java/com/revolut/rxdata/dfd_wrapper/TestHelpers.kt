package com.revolut.rxdata.dfd_wrapper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.time.Duration

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

// inspired by Kotest
infix fun <T, U : T> T.shouldBe(expected: U?) = assertEquals(expected, this)

fun Job.assertNotComplete() {
    assertFalse(isCompleted)
}

fun Job.assertComplete() {
    assertTrue(isCompleted)
}

fun <T> Flow<T>.firstOrError() = flow { emit(first()) }

fun <T> Flow<T>.test(scope: CoroutineScope): TestObserver<T> {
    return TestObserver(this, scope)
}

// inspired by https://proandroiddev.com/from-rxjava-to-kotlin-flow-testing-42f1641d8433
class TestObserver<T>(
    flow: Flow<T>,
    scope: CoroutineScope,
) {
    private val values = mutableListOf<T>()

    @Volatile
    private var savedError: Throwable? = null
    private val job: Job = scope.launch {
        try {
            flow.collect {
                values.add(it)
            }
        } catch (e: Throwable) {
            savedError = e
        }
    }

    fun finish() {
        job.cancel()
    }

    fun assertTerminated(): TestObserver<T> {
        assertTrue(job.isCompleted)
        return this
    }

    fun assertComplete(): TestObserver<T> {
        assertTrue(job.isCompleted)
        savedError shouldBe null
        return this
    }

    fun assertNoValues(): TestObserver<T> {
        this.values shouldBe emptyList()
        return this
    }

    fun assertValues(vararg values: T): TestObserver<T> {
        this.values shouldBe values.toList()
        return this
    }

    fun assertValueCount(count: Int): TestObserver<T> {
        values.size shouldBe count
        return this
    }

    fun assertValueAt(index: Int, value: T): TestObserver<T> {
        values[index] shouldBe value
        return this
    }

    fun assertError(error: Throwable): TestObserver<T> {
        // comparing error to savedError directly causes false negatives,
        // because exceptions may be wrapped when coroutines hop between threads
        savedError?.rootCause shouldBe error.rootCause
        return this
    }

    suspend fun awaitTerminalEvent(duration: Duration): Boolean = try {
        withTimeout(duration) {
            job.join()
        }
        true
    } catch (e: TimeoutCancellationException) {
        job.cancel()
        false
    }

    private val Throwable.rootCause get() = generateSequence(this) { it.cause }.last()
}