package com.revolut.flowdata.extensions

import com.revolut.data.model.Data
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter

fun <T> Flow<Data<T>>.filterContent(condition: (T) -> Boolean): Flow<Data<T>> =
    filter { data -> data.content?.let { condition(it) } ?: true }