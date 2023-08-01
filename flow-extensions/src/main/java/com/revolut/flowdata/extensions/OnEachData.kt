package com.revolut.flowdata.extensions

import com.revolut.data.model.Data
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

fun <T> Flow<Data<T>>.doOnDataError(onError: (Throwable) -> Unit): Flow<Data<T>> =
    onEach { data -> data.error?.let { onError(it) } }

fun <T> Flow<Data<T>>.doOnLoaded(onContent: (T) -> Unit): Flow<Data<T>> =
    onEach { data ->
        val content = data.content
        if (!data.loading && data.error == null && content != null) {
            onContent(content)
        }
    }