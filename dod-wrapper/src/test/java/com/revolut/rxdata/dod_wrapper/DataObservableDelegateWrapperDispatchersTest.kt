package com.revolut.rxdata.dod_wrapper

import app.cash.turbine.test
import com.revolut.data.model.Data
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.coroutines.ContinuationInterceptor
import kotlin.time.Duration.Companion.seconds

@ExperimentalCoroutinesApi
class DataObservableDelegateWrapperDispatchersTest {

    @Test
    fun `GIVEN wrapper WHEN setDispatchers THEN use test dispatchers`() = runTest(dispatchTimeoutMs = 1_000) {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)

        DataObservableDelegateWrapperDispatchers.setDispatchers(testDispatcher)
        DataObservableDelegateWrapperDispatchers.IO shouldBe testDispatcher
        DataObservableDelegateWrapperDispatchers.Unconfined shouldBe testDispatcher

        lateinit var networkDispatcher: CoroutineDispatcher

        val delegate = DataObservableDelegateWrapper<Unit, String>(
            fromNetwork = {
                delay(3.seconds)
                networkDispatcher = currentCoroutineContext()[ContinuationInterceptor] as CoroutineDispatcher
                "from network"
            },
            fromMemory = { null },
            toMemory = { _, _ -> },
            fromStorage = { null },
            toStorage = { _, _ -> },
        )

        delegate.observe(Unit).test {
            awaitItem() shouldBe Data(loading = true)
            awaitItem() shouldBe Data("from network")
        }
        networkDispatcher shouldBe testDispatcher

        DataObservableDelegateWrapperDispatchers.resetDispatchers()
        DataObservableDelegateWrapperDispatchers.IO shouldBe Dispatchers.IO
        DataObservableDelegateWrapperDispatchers.Unconfined shouldBe Dispatchers.Unconfined
    }
}
