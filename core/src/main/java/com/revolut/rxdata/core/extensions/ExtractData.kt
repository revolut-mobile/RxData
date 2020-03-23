package com.revolut.rxdata.core.extensions

import com.revolut.rxdata.core.Data
import io.reactivex.Observable


fun <T> Observable<Data<T>>.extractData(): Observable<T> =
    filter { it.content != null }.map { it.content!! }

fun <T> Observable<Data<T>>.extractError(): Observable<Data<T>> {
    return flatMap {
        if (it.error != null && it.content == null) {
            Observable.error(it.error)
        } else {
            Observable.just(it)
        }
    }
}

fun <T> Observable<Data<T>>.extractDataOrError(): Observable<T> = extractError().extractData()

fun <T> Observable<Data<T>>.extractErrorStrict(): Observable<Data<T>> = flatMap {
    if (it.error != null) {
        Observable.error(it.error)
    } else {
        Observable.just(it)
    }
}

fun <T> Observable<Data<T>>.extractDataIfLoaded(): Observable<T> {
    return filter { it.content != null && !it.loading }
        .map { it.content!! }
}
