package com.revolut.flowdata.extensions

import com.revolut.data.model.Data
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/*
 * Copyright (C) 2022 Revolut
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

fun <A : Any, B : Any> combineLatestData(
    a: Flow<Data<A>>,
    b: Flow<Data<B>>
): Flow<Data<Pair<A, B>>> = combine(a, b) { _a, _b ->
    val aContent = _a.content
    val bContent = _b.content

    val data: Pair<A, B>? = when {
        aContent != null && bContent != null -> aContent to bContent
        else -> null
    }

    val error = listOfNotNull(_a.error, _b.error).takeUnless { it.isEmpty() }?.let {
        CompositeException(it)
    }
    Data(
        content = data,
        loading = _a.loading || _b.loading,
        error = error
    )
}


fun <A : Any, B : Any, C : Any> combineLatestData(
    a: Flow<Data<A>>,
    b: Flow<Data<B>>,
    c: Flow<Data<C>>
): Flow<Data<Triple<A, B, C>>> = combineLatestData(combineLatestData(a, b), c)
    .mapData { (ab, c) -> Triple(ab.first, ab.second, c) }