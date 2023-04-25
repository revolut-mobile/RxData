package com.revolut.rxdata.dod

import com.revolut.data.model.Data
import io.reactivex.Single
import org.junit.jupiter.api.Assertions.assertEquals
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

        val upstreamEmissions = ArrayList<Data<Domain>>()

        dataObservableDelegate.observe(params = params, forceReload = forceReload).take(100)
            .doOnNext { upstreamEmissions.add(it) }
            .switchMap {
                dataObservableDelegate.observe(params = params, forceReload = true).take(100)
            }
            .test()
            .apply { ioScheduler.triggerActions() }

        assertEquals(
            listOf(
                Data(null, null, true),
                // 1st iteration
                Data(cachedDomain, null, true),
                Data(cachedDomain, null, false),
                // 2nd iteration
                Data(cachedDomain, null, true),
                Data(cachedDomain, null, false),
                // no emits after this point
            ), upstreamEmissions
        )
    }

    @Test
    fun `WHEN dod switchMaps to the same forceReload dod AND fromNetwork returns errors THEN emissions are muted after 2nd iteration`() {
        whenever(fromNetwork.invoke(eq(params))).thenReturn(Single.fromCallable { throw backendException })
        storage[params] = cachedDomain
        memCache.remove(params)

        val upstreamEmissions = ArrayList<Data<Domain>>()

        dataObservableDelegate.observe(params = params).take(100)
            .doOnNext { upstreamEmissions.add(it) }
            .switchMap {
                dataObservableDelegate.observe(params = params, forceReload = true).take(100)
            }
            .test()
            .apply { ioScheduler.triggerActions() }

        assertEquals(
            listOf(
                Data(null, null, true),
                // 1st iteration
                Data(cachedDomain, null, true),
                Data(cachedDomain, backendException, false),
                // 2nd iteration
                Data(cachedDomain, null, true),
                Data(cachedDomain, backendException, false),
                // no emits after this point
            ), upstreamEmissions
        )
    }

}
