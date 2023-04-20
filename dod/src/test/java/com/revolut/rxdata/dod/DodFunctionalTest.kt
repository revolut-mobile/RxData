package com.revolut.rxdata.dod

import com.revolut.rxdata.core.extensions.takeUntilLoaded
import io.reactivex.Single
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.*
import java.lang.IllegalStateException
import java.lang.Thread.sleep
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

@TestInstance(PER_CLASS)
class DodFunctionalTest {

    private val networkCallDurationMillis = 300L
    private val maxNetworkEventAwaitMillis = networkCallDurationMillis * 2

    private val fromNetwork: (Unit) -> Single<String> = mock()

    private val fromNetworkScoped: DataObservableDelegate<Unit, String>.(Unit) -> Single<String> =
        { fromNetwork(it) }

    private val toMemory: (Unit, String) -> Unit = { _, _ -> }
    private val fromMemory: (Unit) -> String = { "domain_$it" }
    private val toStorage: (Unit, String) -> Unit = { _, _ -> }
    private val fromStorage: (Unit) -> String = { "domain_$it" }

    private val dataObservableDelegate = DataObservableDelegate(
        fromNetwork = fromNetworkScoped,
        fromMemory = fromMemory,
        toMemory = toMemory,
        fromStorage = fromStorage,
        toStorage = toStorage
    )

    private val noCacheOfflineDod = DataObservableDelegate(
        fromNetwork = { Single.error(IllegalStateException("no network")) },
        fromMemory = { null },
        toMemory = toMemory,
        fromStorage = { null },
        toStorage = toStorage
    )
    private val noCacheOnlineDod = DataObservableDelegate<Unit, String>(
        fromNetwork = { Single.fromCallable { "test" } },
        fromMemory = { null },
        toMemory = toMemory,
        fromStorage = { null },
        toStorage = toStorage
    )

    init {
        whenever(fromNetwork.invoke(Unit)).thenReturn(Single.fromCallable {
            println("Network Request subscribed")
            sleep(networkCallDurationMillis)
            println("Network Request emitting")
            "domain_model"
        })
    }

    companion object {

        @JvmStatic
        fun params(): List<Int> = (1..10).toList()

    }

    @ParameterizedTest
    @MethodSource("params")
    @Execution(CONCURRENT)
    fun `WHEN dod concurrently subscribed THEN all subscribers should receive terminal event`(attempt: Int) {
        // given
        sleep(attempt * 5L)

        // when
        val testSubscription = dataObservableDelegate.observe(Unit, forceReload = true)
            .takeUntilLoaded()
            .test()

        testSubscription.awaitTerminalEvent(maxNetworkEventAwaitMillis, MILLISECONDS)

        // then
        testSubscription.assertTerminated()
    }

    @RepeatedTest(5)
    fun attemptRaceCondition() {
        val completedCounter = AtomicInteger(0)
        val count = 20

        (1..count).map {
            sleep(networkCallDurationMillis / count)
            Thread {
                dataObservableDelegate.observe(Unit, forceReload = true)
                    .takeUntilLoaded()
                    .doOnComplete {
                        completedCounter.incrementAndGet()
                    }
                    .test()
                    .awaitTerminalEvent(networkCallDurationMillis * 2, SECONDS)
            }.apply { start() }
        }.forEach {
            it.join(maxNetworkEventAwaitMillis)
        }

        assert(completedCounter.get() == count) { "Only ${completedCounter.get()} streams terminated"}
    }

    @RepeatedTest(1000)
    fun attemptNoCacheNoNetworkRaceCondition() {
        val count = 3
        val maxThreadDelay = 3
        val maxDelay: Long = (count + 2).toLong() * maxThreadDelay + 30
        val isLastEmitLoadingMap = ConcurrentHashMap<Int, Boolean>(count)

        (1..count).map { num ->
            val delay = Random.nextInt(0, maxThreadDelay).toLong()
            Thread.sleep(delay)
            Thread {
                noCacheOfflineDod.observe(Unit, forceReload = true)
                    .doOnNext {
                        println("Stream $num: data emitted $it")
                        isLastEmitLoadingMap[num] = it.loading
                    }
                    .test()
                    .awaitTerminalEvent(maxDelay, MILLISECONDS)
            }.apply { start() }
        }.forEach {
            it.join(maxDelay)
        }

        val finishedWithLoadingTrueCount = isLastEmitLoadingMap.count { it.value }
        assert(finishedWithLoadingTrueCount == 0) { "Finished with loading true count = $finishedWithLoadingTrueCount" }
    }

    @Disabled("waiting for fix")
    @RepeatedTest(1000)
    fun attemptNoCacheOnlineRaceCondition() {
        val count = 3
        val maxThreadDelay = 3
        val maxDelay: Long = (count + 2).toLong() * maxThreadDelay + 30
        val isLastEmitLoadingMap = ConcurrentHashMap<Int, Boolean>(count)

        (1..count).map { num ->
            val delay = Random.nextInt(0, maxThreadDelay).toLong()
            sleep(delay)
            Thread {
                noCacheOnlineDod.observe(Unit, forceReload = true)
                    .doOnNext {
                        println("Stream $num: data emitted $it")
                        isLastEmitLoadingMap[num] = it.loading
                    }
                    .test()
                    .awaitTerminalEvent(maxDelay, MILLISECONDS)
            }.apply { start() }
        }.forEach {
            it.join(maxDelay)
        }

        val finishedWithLoadingTrueCount = isLastEmitLoadingMap.count { it.value }
        assert(finishedWithLoadingTrueCount == 0) { "Finished with loading true count = $finishedWithLoadingTrueCount" }
    }

}
