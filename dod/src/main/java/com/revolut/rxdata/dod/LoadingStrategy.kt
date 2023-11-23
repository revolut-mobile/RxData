package com.revolut.rxdata.dod

/**
 * [DataObservableDelegate] observation loading strategy
 */
sealed class LoadingStrategy(
    val refreshMemory: Boolean,
    val refreshStorage: Boolean,
) {
    /**
     * data will be fetched from the Network even if the data exists in the cache
     */
    object ForceReload: LoadingStrategy(
        refreshMemory = true,
        refreshStorage = true,
    )

    /**
     * data will not be fetched from the Network if the data exists in the memory cache
     */
    object Auto: LoadingStrategy(
        refreshMemory = false,
        refreshStorage = true,
    )

    /**
     * data will not be fetched from the Network if the data exists in the cache memory or storage
     */
    object LazyReload: LoadingStrategy(
        refreshMemory = false,
        refreshStorage = false,
    )
}