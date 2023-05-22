package com.revolut.rxdata.dod

import io.reactivex.Observable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep
import java.util.concurrent.atomic.AtomicInteger

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
@Suppress("CheckResult")
class SharedObservableRequestTest {

    private var loadCounter = AtomicInteger(0)

    @Test
    fun `when requested first time then loading started`() {
        val cache = createCache()

        cache.getOrLoad(params = "1").subscribe()
        sleep(150)

        assertEquals(1, loadCounter.get())
    }

    @Test
    fun `when request 2 times one by one then only one loading started`() {
        val cache = createCache()
        lateinit var result1: String
        lateinit var result2: String

        cache.getOrLoad(params = "1").subscribe { result -> result1 = result }
        cache.getOrLoad(params = "1").subscribe { result -> result2 = result }
        sleep(150)

        assertEquals(1, loadCounter.get())
        assertEquals(result1, result2)
    }

    @Test
    fun  `when request 2 times with same key and 1 time with different key one by one then 2 loadings started`() {
        val cache = createCache()

        lateinit var result1: String
        lateinit var result2: String
        lateinit var result3: String

        cache.getOrLoad(params = "1").subscribe { result -> result1 = result }
        cache.getOrLoad(params = "1").subscribe { result -> result2 = result }
        cache.getOrLoad(params = "2").subscribe { result -> result3 = result }
        sleep(150)

        assertEquals(2, loadCounter.get())
        assertEquals(result1, result2)
        assertNotEquals(result1, result3)
    }

    @Test
    fun `when request 2 times one by one with different keys then both loading started`() {
        val cache = createCache()
        lateinit var result1: String
        lateinit var result2: String

        cache.getOrLoad(params = "1").subscribe { result -> result1 = result }
        cache.getOrLoad(params = "2").subscribe { result -> result2 = result }
        sleep(150)

        assertEquals(2, loadCounter.get())
        assertNotEquals(result1, result2)
    }

    @Test
    fun `when request 2 times with waiting then both loading started`() {
        val cache = createCache()
        lateinit var result1: String
        lateinit var result2: String

        cache.getOrLoad(params = "1").subscribe { result -> result1 = result }
        sleep(150)
        cache.getOrLoad(params = "1").subscribe { result -> result2 = result }
        sleep(150)
        
        assertEquals(2, loadCounter.get())
        assertEquals(result1, result2)
    }

    private fun createCache(): SharedObservableRequest<String, String> = SharedObservableRequest(
        load = { params ->
            Observable.fromCallable {
                sleep(100)
                "$params result"
            }.doOnSubscribe {
                loadCounter.incrementAndGet()
            }
        }
    )
}