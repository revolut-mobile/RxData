package com.revolut.rxdata.core.extensions

import com.revolut.rxdata.core.Data
import io.reactivex.Observable

fun <T> Observable<Data<T>>.takeUntilLoaded(): Observable<Data<T>> =
    takeUntil { data -> !data.loading }

fun <T> Observable<Data<T>>.filterWhileLoading(): Observable<Data<T>> =
    filter { data -> !data.loading }

fun <T> Observable<Data<T>>.skipWhileLoading(): Observable<Data<T>> =
    skipWhile { data -> data.loading }
