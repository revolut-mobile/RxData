package com.revolut.rxdata.dod

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.TestScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

/*
 * Copyright (C) 2019 Revolut
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

class SharedObservableRequestTest {

    private var loadCounter = 0

    private val ioScheduler: TestScheduler = TestScheduler()

    @Before
    fun setUp() {
        RxJavaPlugins.setIoSchedulerHandler { ioScheduler }
    }

    @Test
    fun `when requested first time then loading started`() {
        val cache = createCache()

        cache.getOrLoad(params = "1").subscribe()
        ioScheduler.triggerActions()

        assertEquals(1, loadCounter)
    }

    @Test
    fun `when request 2 times one by one then only one loading started`() {
        val cache = createCache()
        lateinit var result1: Any
        lateinit var result2: Any

        cache.getOrLoad(params = "1").subscribe { result -> result1 = result }
        cache.getOrLoad(params = "1").subscribe { result -> result2 = result }
        ioScheduler.triggerActions()

        assertEquals(1, loadCounter)
        assertEquals(result1, result2)
    }

    @Test
    fun `when request 2 times with same key and 1 time with different key one by one then 2 loadings started`() {
        val cache = createCache()
        lateinit var result1: Any
        lateinit var result2: Any
        lateinit var result3: Any

        cache.getOrLoad(params = "1").subscribe { result -> result1 = result }
        cache.getOrLoad(params = "1").subscribe { result -> result2 = result }
        cache.getOrLoad(params = "2").subscribe { result -> result3 = result }
        ioScheduler.triggerActions()

        assertEquals(2, loadCounter)
        assertEquals(result1, result2)
        assertNotEquals(result1, result3)
    }

    @Test
    fun `when request 2 times one by one with different keys then both loading started`() {
        val cache = createCache()
        lateinit var result1: Any
        lateinit var result2: Any

        cache.getOrLoad(params = "1").subscribe { result -> result1 = result }
        cache.getOrLoad(params = "2").subscribe { result -> result2 = result }
        ioScheduler.triggerActions()

        assertEquals(2, loadCounter)
        assertNotEquals(result1, result2)
    }

    @Test
    fun `when request 2 times with waiting then both loading started`() {
        val cache = createCache()
        lateinit var result1: Any
        lateinit var result2: Any

        cache.getOrLoad(params = "1").subscribe { result -> result1 = result }
        ioScheduler.triggerActions()
        cache.getOrLoad(params = "1").subscribe { result -> result2 = result }
        ioScheduler.triggerActions()

        assertEquals(2, loadCounter)
        assertNotEquals(result1, result2)
    }

    private fun createCache(): SharedObservableRequest<Any, Any> {
        return SharedObservableRequest(
            load = { Observable.just(Any().also { loadCounter++ }) }
        )
    }
}