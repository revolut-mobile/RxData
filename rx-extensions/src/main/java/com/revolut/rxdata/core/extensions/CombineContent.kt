package com.revolut.rxdata.core.extensions

import com.revolut.data.model.Data
import io.reactivex.Observable
import io.reactivex.exceptions.CompositeException

fun <A : Any, B : Any> combineLatestContent(
    a: Observable<Data<A>>,
    b: Observable<Data<B>>
): Observable<Data<Pair<A, B>>> = Observable.combineLatest(a, b) { _a, _b ->
    val aContent = _a.content
    val bContent = _b.content

    val data: Pair<A, B>? = when {
        aContent != null && bContent != null -> aContent to bContent
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
}


fun <A : Any, B : Any, C : Any> combineLatestContent(
    a: Observable<Data<A>>,
    b: Observable<Data<B>>,
    c: Observable<Data<C>>
): Observable<Data<Triple<A, B, C>>> = combineLatestContent(combineLatestContent(a, b), c)
    .mapData { (ab, c) -> Triple(ab.first, ab.second, c) }