package com.revolut.rxdata.dod

/**
 * [DataObservableDelegate] observation loading strategy
 *
 * @param refreshMemory - if true data will be fetched from network even if there is something in the memory
 * @param refreshStorage - if true data will be fetched from network even if there is something in the storage
 */
sealed class LoadingStrategy(
    internal val refreshMemory: Boolean,
    internal val refreshStorage: Boolean,
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