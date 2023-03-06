package com.revolut.rxdata.dfd_wrapper

import app.cash.turbine.test
import com.revolut.data.model.Data
import com.revolut.flowdata.extensions.takeUntilLoaded
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.RepetitionInfo
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

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
@TestInstance(PER_CLASS)
class DfdFunctionalTest {

    private val networkCallDurationMillis = 300L

    private val fromNetwork: suspend (Unit) -> Domain = {
        // can't use `delay` here: under high load sometimes it doesn't wake in time & fails the test
        @Suppress("BlockingMethodInNonBlockingContext")
        Thread.sleep(networkCallDurationMillis)
        "domain_model"
    }
    private val toMemory: (Unit, String) -> Unit = { _, _ -> }
    private val fromMemory: (Unit) -> String = { "domain_$it" }
    private val toStorage: (Unit, String) -> Unit = { _, _ -> }
    private val fromStorage: (Unit) -> String = { "domain_$it" }

    private val dataFlowDelegate = DataFlowDelegate(
        fromNetwork = fromNetwork,
        fromMemory = fromMemory,
        toMemory = toMemory,
        fromStorage = fromStorage,
        toStorage = toStorage
    )

    private val noNetworkError = IllegalStateException("no network")

    private val noCacheOfflineDfd = DataFlowDelegate(
        fromNetwork = { throw noNetworkError },
        fromMemory = { null },
        toMemory = toMemory,
        fromStorage = { null },
        toStorage = toStorage
    )

    companion object {

        @JvmStatic
        fun params(): List<Int> = (1..10).toList()

    }

    private fun realLifeTest(testBody: suspend CoroutineScope.() -> Unit) = runTest(
        dispatchTimeoutMs = 2_000,
        testBody = {
            withContext(Dispatchers.IO) {  // prevents skipping delays
                testBody()
            }
        },
    )

    @ParameterizedTest
    @MethodSource("params")
    @Execution(CONCURRENT)
    fun `WHEN dfd concurrently subscribed THEN all subscribers should receive terminal event`(attempt: Int) = realLifeTest {
        // given
        delay(attempt * 5L)

        // when
        dataFlowDelegate.observe(Unit, forceReload = true)
            .takeUntilLoaded()
            .collect()

        // then is implied by runTest & dispatchTimeoutMs
    }

    @RepeatedTest(5)
    fun attemptRaceCondition() = realLifeTest {
        val count = 20
        (1..count).map {
            delay(networkCallDurationMillis / count)
            launch {
                dataFlowDelegate.observe(Unit, forceReload = true)
                    .takeUntilLoaded()
                    .collect()
            }
        }
        // completion of all collectors is ensured by runTest & dispatchTimeoutMs
    }

    @RepeatedTest(1000)
    fun attemptNoCacheNoNetworkRaceCondition1() = realLifeTest {
        (1..3).map {
            delay(Random.nextInt(3).milliseconds)
            launch {
                noCacheOfflineDfd.observe(Unit, forceReload = true)
                    .test {
                        awaitItem() shouldBe Data(loading = true)
                        awaitItem() shouldBe Data(loading = false, error = noNetworkError)
                        cancelAndIgnoreRemainingEvents()
                    }
            }
        }
    }

    @RepeatedTest(1000)
    fun attemptNoCacheNoNetworkRaceCondition2() = realLifeTest {
        (1..3).map {
            delay(Random.nextInt(3).milliseconds)
            launch {
                noCacheOfflineDfd.observe(Unit, forceReload = true)
                    .take(2)
                    .toList()
                    .shouldBe(
                        listOf(
                            Data(loading = true),
                            Data(loading = false, error = noNetworkError),
                        )
                    )
            }
        }
    }

    @RepeatedTest(1000)
    fun attemptNoCacheNoNetworkRaceCondition3(repetitionInfo: RepetitionInfo) = realLifeTest {
        val count = 3
        val latestValueMap = ConcurrentHashMap<Int, Data<Unit>>(count)

        val jobs = (1..count).map { num ->
            delay(Random.nextInt(3).milliseconds)
            launch {
                noCacheOfflineDfd.observe(Unit, forceReload = true)
                    .collect {
                        latestValueMap[num] = it
                    }
            }
        }

        // first iteration takes 10-20x more time than subsequent ones
        val delay = if (repetitionInfo.currentRepetition == 1) 200 else 20
        delay(delay.milliseconds)
        jobs.forEach {
            it.cancel()
        }

        (1..count).forEach {
            latestValueMap[it] shouldBe Data(loading = false, error = noNetworkError)
        }
    }
}