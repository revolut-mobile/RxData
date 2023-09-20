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

fun <T : Any> combineContent(
    flows: Iterable<Flow<Data<T>>>
) = combine(flows) { data ->
    val content: List<T>? = when {
        data.all { it.content != null } -> data.mapNotNull { it.content }
        else -> null
    }
    val loading = data.any { it.loading }
    val error = data.mapNotNull { it.error }.takeUnless { it.isEmpty() }?.let {
        CompositeException(it)
    }
    Data(
        content = content,
        loading = loading,
        error = error,
    )
}

fun <T : Any> combineContent(
    vararg flows: Flow<Data<T>>
): Flow<Data<List<T>>> = combineContent(flows.toList())
