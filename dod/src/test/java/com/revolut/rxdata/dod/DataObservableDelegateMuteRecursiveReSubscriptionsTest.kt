package com.revolut.rxdata.dod

import io.reactivex.Single
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever


class DataObservableDelegateMuteRecursiveReSubscriptionsTest : BaseDataObservableDelegateTest() {

    @ValueSource(booleans = [true, false])
    @ParameterizedTest
    fun `WHEN dod switchMaps to the same forceReload dod THEN emissions are muted after 2nd iteration`(forceReload: Boolean) {
        whenever(fromNetwork.invoke(eq(params))).thenReturn(Single.fromCallable { cachedDomain })
        storage[params] = cachedDomain
        memCache.remove(params)

        dataObservableDelegate.observe(params = params, forceReload = forceReload).take(100)
            .switchMap {
                dataObservableDelegate.observe(params = params, forceReload = true).take(100)
            }
            .test()
            .apply {
                ioScheduler.triggerActions()
            }
            .assertValueCount(8)
    }

    @Test
    fun `WHEN dod switchMaps to the same forceReload dod AND fromNetwork returns errors THEN emissions are muted after 2nd iteration`() {
        whenever(fromNetwork.invoke(eq(params))).thenReturn(Single.fromCallable { throw backendException })
        storage[params] = cachedDomain
        memCache.remove(params)

        dataObservableDelegate.observe(params = params).take(100)
            .switchMap {
                dataObservableDelegate.observe(params = params, forceReload = true).take(100)
            }
            .test()
            .apply {
                ioScheduler.triggerActions()
            }
            .assertValueCount(8)
    }

}
