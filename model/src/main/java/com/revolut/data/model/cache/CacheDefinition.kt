package com.revolut.data.model.cache

interface CacheDefinition {
    fun <T : Any> toMemory(key: String, value: T)
    fun <T> fromMemory(key: String): T?

    fun <T : Any> toStorage(key: String, value: T)
    fun <T> fromStorage(key: String): T?

    fun <T> remove(key: String)
}

object EmptyCacheDefinition : CacheDefinition {
    override fun <T : Any> toMemory(key: String, value: T) {
        //no-op
    }

    override fun <T> fromMemory(key: String): T? {
        //no-op
        return null
    }

    override fun <T : Any> toStorage(key: String, value: T) {
        //no-op
    }

    override fun <T> fromStorage(key: String): T? {
        //no-op
        return null
    }

    override fun <T> remove(key: String) {
        //no-op
    }
}

