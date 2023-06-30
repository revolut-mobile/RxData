package com.revolut.flowdata.extensions

import com.revolut.data.model.Data
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

fun <A : Any, B : Any> combineContent(
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

fun <A : Any, B : Any, C : Any> combineContent(
    a: Flow<Data<A>>,
    b: Flow<Data<B>>,
    c: Flow<Data<C>>
): Flow<Data<Triple<A, B, C>>> = combineContent(combineContent(a, b), c)
    .mapData { (ab, c) -> Triple(ab.first, ab.second, c) }