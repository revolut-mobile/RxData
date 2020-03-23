package com.revolut.rxdata.core.extensions

import com.revolut.rxdata.core.Data
import io.reactivex.Observable

fun <T, R> Observable<Data<T>>.mapData(block: (T) -> R?): Observable<Data<R>> =
    map { data -> data.mapData(block) }

