package com.revolut.rxdata.core.extensions

import com.revolut.data.model.Data
import io.reactivex.Observable

@Deprecated(
    message = "The method name is misleading. Use newer version combineLatestContent",
    replaceWith = ReplaceWith("com.revolut.rxdata.core.extensions.combineLatestContent")
)
fun <A : Any, B : Any> combineLatestData(
    a: Observable<Data<A>>,
    b: Observable<Data<B>>
): Observable<Data<Pair<A, B>>> = combineLatestContent(a, b)

@Deprecated(
    message = "The method name is misleading. Use newer version combineLatestContent",
    replaceWith = ReplaceWith("com.revolut.rxdata.core.extensions.combineLatestContent")
)
fun <A : Any, B : Any, C : Any> combineLatestData(
    a: Observable<Data<A>>,
    b: Observable<Data<B>>,
    c: Observable<Data<C>>
): Observable<Data<Triple<A, B, C>>> = combineLatestContent(a, b, c)
