package com.revolut.rxdata.dod

import java.util.*


internal class ShiftingTriple<E> {

    private var _lastElement: E? = null

    val lastElement: E
        get() = _lastElement ?: throw NoSuchElementException("Triple is empty")

    val size: Int
        get() =  hashCodesVector.size

    private val hashCodesVector = Vector<Int>(3)

    @Synchronized
    fun add(element: E): ShiftingTriple<E> {
        _lastElement = element
        if (3 != hashCodesVector.size) {
            hashCodesVector.add(element.hashCode())
        } else {
            hashCodesVector[0] = hashCodesVector[1]
            hashCodesVector[1] = hashCodesVector[2]
            hashCodesVector[2] = element.hashCode()
        }
        return this
    }

    operator fun get(index: Int): Int = hashCodesVector[index]

    fun firstHashCode(): Int = hashCodesVector.first()

    fun lastHashCode(): Int = hashCodesVector.last()

    fun middleHashCode(): Int = if (hashCodesVector.size == 3) hashCodesVector[1] else throw
            IllegalStateException("No middle element for ${hashCodesVector.size} elements")

}