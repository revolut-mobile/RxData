package com.revolut.rxdata.core.extensions

import com.revolut.rxdata.core.Data
import io.reactivex.Observable

fun <T> Observable<Data<T>>.doOnDataError(onError: (Throwable) -> Unit): Observable<Data<T>> =
    doOnNext { data ->
        data.error?.let { onError(it) }
    }

fun <T> Observable<Data<T>>.doOnLoaded(onNext: (T) -> Unit): Observable<Data<T>> =
    doOnNext { data ->
        if (!data.loading && data.error == null && data.content != null) {
            onNext(data.content)
        }
    }