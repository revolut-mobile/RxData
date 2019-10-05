package com.revolut.rxdata.dod

data class Data<out T>(
    val content: T? = null,
    val error: Throwable? = null,
    val loading: Boolean = false
)