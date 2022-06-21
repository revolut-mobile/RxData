package com.revolut.rxdata.dod

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.revolut.rxdata.core.extensions.takeUntilLoaded
import io.reactivex.Single
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger

class DodFunctionalTest {

    private val domain: String = "domain_model"

    private lateinit var fromNetwork: (Unit) -> Single<String>

    private val fromNetworkScoped: DataObservableDelegate<Unit, String>.(Unit) -> Single<String> =
        { fromNetwork(it) }

    private val toMemory: (Unit, String) -> Unit = { _, _ -> }
    private val fromMemory: (Unit) -> String = { "domain_$it" }
    private val toStorage: (Unit, String) -> Unit = { _, _ -> }
    private val fromStorage: (Unit) -> String = { "domain_$it" }

    private lateinit var dataObservableDelegate: DataObservableDelegate<Unit, String>

    private val computationScheduler = Schedulers.computation()
    private val ioScheduler = Schedulers.io()

    @BeforeEach
    fun before() {
        fromNetwork = mock()

        dataObservableDelegate = DataObservableDelegate(
            fromNetwork = fromNetworkScoped,
            fromMemory = fromMemory,
            toMemory = toMemory,
            fromStorage = fromStorage,
            toStorage = toStorage
        )

        RxJavaPlugins.setIoSchedulerHandler { ioScheduler }
        RxJavaPlugins.setComputationSchedulerHandler { computationScheduler }

        whenever(fromNetwork.invoke(Unit)).thenReturn(Single.fromCallable {
            sleep(1000)
            domain
        })
    }

    @RepeatedTest(5)
    fun attemptRaceCondition() {
        // schedule 20 DODs simultaneously reloading the same network request
        val completedCounter = AtomicInteger(0)
        val count = 20

        (1..count).map {
            sleep(50)
            val thread = Thread {
                dataObservableDelegate.observe(Unit, forceReload = true)
                    .takeUntilLoaded()
                    .doOnComplete {
                        completedCounter.incrementAndGet()
                    }
                    .test()
                    .awaitTerminalEvent(300, SECONDS)
            }
            thread.start()
            thread
        }.forEach {
            it.join(1000)
        }

        assert(completedCounter.get() == count) { "Only $completedCounter streams completed" }
    }

}