package com.revolut.rxdata.dod

import com.revolut.data.model.Data
import io.reactivex.Single
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicInteger

class DataObservableDelegateSharedStorageRequestTest : BaseDataObservableDelegateTest() {

    private val domain: Domain = "domain_model"

    @Suppress("CheckResult")
    @Test
    fun `WHEN memoryIsEmpty and dod is observed multiple times THEN fromStorage is called once`() {
        val counter = AtomicInteger(0)

        dataObservableDelegate = DataObservableDelegate(
            fromNetwork = fromNetworkScoped,
            fromMemory = fromMemory,
            toMemory = toMemory,
            fromStorageSingle = {
                Single.fromCallable {
                    counter.incrementAndGet()
                    Data(cachedDomain)
                }.delay(100L, MILLISECONDS, ioScheduler)
            },
            toStorage = toStorage
        )

        whenever(fromNetwork.invoke(eq(params))).thenReturn(Single.fromCallable { domain })

        dataObservableDelegate.observe(params = params, forceReload = true).test()
        dataObservableDelegate.observe(params = params, forceReload = true).test()
        dataObservableDelegate.observe(params = params, forceReload = true).test()

        ioScheduler.triggerActions()

        assertEquals(1, counter.get())
    }

}