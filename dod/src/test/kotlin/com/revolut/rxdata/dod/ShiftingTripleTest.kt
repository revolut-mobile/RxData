package com.revolut.rxdata.dod

import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftingTripleTest {


    @Test(expected = NoSuchElementException::class)
    fun `WHEN first called and No elements THEN NoSuchElementException`() {
        val triple = ShiftingTriple<String>()

        triple.firstHashCode()
    }

    @Test(expected = NoSuchElementException::class)
    fun `WHEN last called and No elements THEN NoSuchElementException`() {
        val triple = ShiftingTriple<String>()

        triple.lastHashCode()
    }

    @Test(expected = NoSuchElementException::class)
    fun `WHEN lastElement called and No elements THEN NoSuchElementException`() {
        val triple = ShiftingTriple<String>()

        triple.lastElement
    }

    @Test(expected = IllegalStateException::class)
    fun `WHEN no elements THEN IllegalStateException for middleHashCode`() {
        val triple = ShiftingTriple<String>()

        triple.middleHashCode()
    }


    @Test(expected = IllegalStateException::class)
    fun `WHEN one element THEN IllegalStateException for middleHashCode`() {
        val triple = ShiftingTriple<String>()
        triple.add("a")

        triple.middleHashCode()
    }

    @Test(expected = IllegalStateException::class)
    fun `WHEN two elements THEN IllegalStateException for middleHashCode`() {
        val triple = ShiftingTriple<String>()
        triple.add("a")
        triple.add("b")

        triple.middleHashCode()
    }

    @Test
    fun `WHEN one element`() {
        val triple = ShiftingTriple<String>()
        triple.add("a")

        assertTrue(triple.lastElement == "a")
        assertTrue(triple.firstHashCode() == "a".hashCode())
        assertTrue(triple.lastHashCode() == "a".hashCode())
    }

    @Test
    fun `WHEN two elements`() {
        val triple = ShiftingTriple<String>()
        triple.add("a")
        triple.add("b")

        assertTrue(triple.lastElement == "b")
        assertTrue(triple.firstHashCode() == "a".hashCode())
        assertTrue(triple.lastHashCode() == "b".hashCode())
    }

    @Test
    fun `WHEN three elements`() {
        val triple = ShiftingTriple<String>()
        triple.add("a")
        triple.add("b")
        triple.add("c")

        assertTrue(triple.lastElement == "c")
        assertTrue(triple.firstHashCode() == "a".hashCode())
        assertTrue(triple.middleHashCode() == "b".hashCode())
        assertTrue(triple.lastHashCode() == "c".hashCode())
    }

    @Test
    fun `WHEN four element added`() {
        val triple = ShiftingTriple<String>()
        triple.add("a")
        triple.add("b")
        triple.add("c")
        triple.add("d")

        assertTrue(triple.lastElement == "d")
        assertTrue(triple.firstHashCode() == "b".hashCode())
        assertTrue(triple.middleHashCode() == "c".hashCode())
        assertTrue(triple.lastHashCode() == "d".hashCode())
    }

}