package com.revolut.data

import com.revolut.data.model.Data
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DataTest {

    @Test
    fun `WHEN errors of same class THEN data is equal`() {
        assertTrue(
            Data("a", loading = true, error = IllegalStateException()) ==
            Data("a", loading = true, error = IllegalStateException())
        )

        assertTrue(
            Data("a", loading = true, error = IllegalStateException()).hashCode() ==
                    Data("a", loading = true, error = IllegalStateException()).hashCode()
        )
    }

    @Test
    fun `WHEN different reasons same class errors THEN data models are not equal`() {
        assertTrue(
            Data("a", loading = true, error = IllegalStateException("a")) !=
                    Data("a", loading = true, error = IllegalStateException("b"))
        )

        assertTrue(
            Data("a", loading = true, error = IllegalStateException("a")) !=
                    Data("a", loading = true, error = IllegalStateException("b"))
        )
    }

    @Test
    fun `WHEN same reasons different class errors THEN data models are not equal`() {
        assertTrue(
            Data("a", loading = true, error = IllegalStateException("a")) !=
                    Data("a", loading = true, error = IllegalArgumentException("a"))
        )

        assertTrue(
            Data("a", loading = true, error = IllegalStateException("a")).hashCode() !=
                    Data("a", loading = true, error = IllegalArgumentException("a")).hashCode()
        )
    }
}