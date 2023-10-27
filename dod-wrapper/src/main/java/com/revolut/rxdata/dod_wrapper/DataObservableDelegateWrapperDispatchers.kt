package com.revolut.rxdata.dod_wrapper

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.jetbrains.annotations.TestOnly

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

object DataObservableDelegateWrapperDispatchers {

    internal lateinit var IO: CoroutineDispatcher
        private set
    internal lateinit var Unconfined: CoroutineDispatcher
        private set

    init {
        resetDispatchers()
    }

    @TestOnly
    fun setDispatchers(dispatcher: CoroutineDispatcher) {
        IO = dispatcher
        Unconfined = dispatcher
    }

    @TestOnly
    fun resetDispatchers() {
        IO = Dispatchers.IO
        Unconfined = Dispatchers.Unconfined
    }
}
