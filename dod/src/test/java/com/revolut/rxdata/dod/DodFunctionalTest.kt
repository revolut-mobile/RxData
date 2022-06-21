package com.revolut.rxdata.dod

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.revolut.rxdata.core.extensions.takeUntilLoaded
import io.reactivex.Single
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger

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

}