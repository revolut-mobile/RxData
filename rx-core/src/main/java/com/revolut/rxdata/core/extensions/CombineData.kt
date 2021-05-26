package com.revolut.rxdata.core.extensions

import com.revolut.rxdata.core.Data
import io.reactivex.Observable
import io.reactivex.Observable.combineLatest
import io.reactivex.exceptions.CompositeException
import io.reactivex.functions.BiFunction


fun <A : Any, B : Any> combineLatestData(
    a: Observable<Data<A>>,
    b: Observable<Data<B>>
): Observable<Data<Pair<A, B>>> = combineLatest(a, b, BiFunction { _a, _b ->
    val data: Pair<A, B>? = when {
        _a.content != null && _b.content != null -> _a.content to _b.content
        else -> null
    }

    val error = listOfNotNull(_a.error, _b.error).takeUnless { it.isEmpty() }?.let {
        CompositeException(*it.toTypedArray())
    }
    Data(
        content = data,
        loading = _a.loading || _b.loading,
        error = error
    )
})


fun <A : Any, B : Any, C : Any> combineLatestData(
    a: Observable<Data<A>>,
    b: Observable<Data<B>>,
    c: Observable<Data<C>>
): Observable<Data<Triple<A, B, C>>> = combineLatestData(combineLatestData(a, b), c)
    .mapData { (ab, c) -> Triple(ab.first, ab.second, c) }
