package com.revolut.rxdata.dod

import io.reactivex.rxjava3.disposables.CompositeDisposable

object DodGlobal {

    internal val disposableContainer = CompositeDisposable()

    private const val DEFAULT_NETWORK_TIMEOUT_SECONDS = 60L

    var networkTimeoutSeconds =
        DEFAULT_NETWORK_TIMEOUT_SECONDS

    fun clearPendingNetwork() = disposableContainer.clear()


}