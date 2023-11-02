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

inline fun <A : Any, B : Any, C : Any, D : Any, R : Any> combineContent(
    a: Flow<Data<A>>,
    b: Flow<Data<B>>,
    c: Flow<Data<C>>,
    d: Flow<Data<D>>,
    crossinline block: suspend (A, B, C, D) -> R,
): Flow<Data<R>> = combineContent(combineContent(a, b, c), d)
    .mapContentSuspended { (abc, d) -> block(abc.first, abc.second, abc.third, d) }

inline fun <A : Any, B : Any, C : Any, D : Any, E : Any, R : Any> combineContent(
    a: Flow<Data<A>>,
    b: Flow<Data<B>>,
    c: Flow<Data<C>>,
    d: Flow<Data<D>>,
    e: Flow<Data<E>>,
    crossinline block: suspend (A, B, C, D, E) -> R,
): Flow<Data<R>> = combineContent(combineContent(a, b, c), combineContent(d, e))
    .mapContentSuspended { (abc, de) -> block(abc.first, abc.second, abc.third, de.first, de.second) }

inline fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, R : Any> combineContent(
    a: Flow<Data<A>>,
    b: Flow<Data<B>>,
    c: Flow<Data<C>>,
    d: Flow<Data<D>>,
    e: Flow<Data<E>>,
    f: Flow<Data<F>>,
    crossinline block: suspend (A, B, C, D, E, F) -> R,
): Flow<Data<R>> = combineContent(combineContent(a, b, c), combineContent(d, e, f))
    .mapContentSuspended { (abc, def) -> block(abc.first, abc.second, abc.third, def.first, def.second, def.third) }

inline fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, G : Any, R : Any> combineContent(
    a: Flow<Data<A>>,
    b: Flow<Data<B>>,
    c: Flow<Data<C>>,
    d: Flow<Data<D>>,
    e: Flow<Data<E>>,
    f: Flow<Data<F>>,
    g: Flow<Data<G>>,
    crossinline block: suspend (A, B, C, D, E, F, G) -> R,
): Flow<Data<R>> = combineContent(combineContent(a, b, c), combineContent(d, e, f), g)
    .mapContentSuspended { (abc, def, g) -> block(abc.first, abc.second, abc.third, def.first, def.second, def.third, g) }

inline fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, G : Any, H : Any, R : Any> combineContent(
    a: Flow<Data<A>>,
    b: Flow<Data<B>>,
    c: Flow<Data<C>>,
    d: Flow<Data<D>>,
    e: Flow<Data<E>>,
    f: Flow<Data<F>>,
    g: Flow<Data<G>>,
    h: Flow<Data<H>>,
    crossinline block: suspend (A, B, C, D, E, F, G, H) -> R,
): Flow<Data<R>> = combineContent(combineContent(a, b, c), combineContent(d, e, f), combineContent(g, h))
    .mapContentSuspended { (abc, def, gh) ->
        block(
            abc.first,
            abc.second,
            abc.third,
            def.first,
            def.second,
            def.third,
            gh.first,
            gh.second
        )
    }

inline fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, G : Any, H : Any, I : Any, R : Any> combineContent(
    a: Flow<Data<A>>,
    b: Flow<Data<B>>,
    c: Flow<Data<C>>,
    d: Flow<Data<D>>,
    e: Flow<Data<E>>,
    f: Flow<Data<F>>,
    g: Flow<Data<G>>,
    h: Flow<Data<H>>,
    i: Flow<Data<I>>,
    crossinline block: suspend (A, B, C, D, E, F, G, H, I) -> R,
): Flow<Data<R>> = combineContent(combineContent(a, b, c), combineContent(d, e, f), combineContent(g, h, i))
    .mapContentSuspended { (abc, def, ghi) ->
        block(
            abc.first,
            abc.second,
            abc.third,
            def.first,
            def.second,
            def.third,
            ghi.first,
            ghi.second,
            ghi.third
        )
    }

fun <T : Any> combineContent(
    vararg flows: Flow<Data<T>>
): Flow<Data<List<T>>> = combineContent(flows.toList())
