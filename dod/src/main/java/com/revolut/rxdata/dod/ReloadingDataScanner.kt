package com.revolut.rxdata.dod

import com.revolut.rxdata.core.Data

class ReloadingDataScanner<T> {

    private var currentData: Data<T>? = null
    private var emitCurrent: Boolean = false
    private var lastLoadingAndErrorHash: Int? = null
    private var lastLoadedContentErrorHash: Int? = null
    private var loadingTimes: Int = 0

    fun registerData(data: Data<T>): ReloadingDataScanner<T> {
        currentData = data

        val currentContentErrorHash = data.contentErrorHash()

        if (data.loading) {
            if (lastLoadingAndErrorHash === null || currentContentErrorHash == lastLoadingAndErrorHash) {
                loadingTimes++
            }
        } else {
            if (currentContentErrorHash != lastLoadingAndErrorHash) {
                loadingTimes = 1
            }
        }

        emitCurrent = (loadingTimes <= 1) && currentContentErrorHash != lastLoadedContentErrorHash

        if (data.loading) {
            lastLoadingAndErrorHash = currentContentErrorHash
        } else {
            lastLoadedContentErrorHash = currentContentErrorHash
        }
        return this
    }

    fun shouldEmitCurrentData(): Boolean = emitCurrent

    fun currentData() = currentData

    private fun Data<T>.contentErrorHash(): Int {
        var result = content?.hashCode() ?: 0
        result = 31 * result + (error?.javaClass?.hashCode() ?: 0)
        result = 31 * result + (error?.message?.hashCode() ?: 0)
        return result
    }


}