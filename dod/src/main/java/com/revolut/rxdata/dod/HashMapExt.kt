package com.revolut.rxdata.dod

import java.util.concurrent.ConcurrentHashMap

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

internal inline fun <K, V> ConcurrentHashMap<K, V>.getOrCreate(key: K, creator: () -> V): V =
    get(key) ?: creator().let { default ->
        val previous = putIfAbsent(key, default)
        previous ?: default
    }
