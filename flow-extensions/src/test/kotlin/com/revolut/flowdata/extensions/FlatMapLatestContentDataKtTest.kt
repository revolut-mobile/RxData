package com.revolut.flowdata.extensions

import app.cash.turbine.test
import com.revolut.data.model.Data
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

@ExperimentalCoroutinesApi
class FlatMapLatestContentDataKtTest {

    companion object {
        @JvmStatic
        fun data() = listOf(
            arguments(true, true, true),
            arguments(true, false, true),
            arguments(false, true, true),
            arguments(false, false, false),
        )

        @JvmStatic
        fun errors() = listOf(
            arguments(null, null, null),
            arguments(TestThrowable("A"), null, TestThrowable("A")),
            arguments(null, TestThrowable("B"), TestThrowable("B")),
            arguments(
                TestThrowable("A"),
                TestThrowable("B"),
                CompositeException(
                    listOf(
                        TestThrowable("A"),
                        TestThrowable("B")
                    )
                )
            ),
        )
    }

    @ParameterizedTest
    @MethodSource("data")
    fun `GIVEN A - loadingA, B - loadingB WHEN switching THEN C - loadingC`(
        loadingA: Boolean,
        loadingB: Boolean,
        loadingC: Boolean,
    ) = runTest {
        val observableA = newDataObservable(loadingA)
        val observableB = newDataObservable(loadingB)

        observableA.flatMapLatestContentDataFlow { observableB }
            .assertLoading(loadingC)
    }

    private fun newDataObservable(loading: Boolean = false, error: Throwable? = null) =
        flowOf(Data("content", error, loading))

    private suspend fun <T> Flow<Data<T>>.assertLoading(loading: Boolean) = test {
        assertEquals(loading, awaitItem().loading)
        awaitComplete()
    }


    @ParameterizedTest
    @MethodSource("errors")
    fun `GIVEN A - errorA, B - errorB WHEN switching THEN C - errorC`(
        errorA: Throwable?,
        errorB: Throwable?,
        errorC: Throwable?,
    ) = runTest {
        val observableA = newDataObservable(error = errorA)
        val observableB = newDataObservable(error = errorB)

        observableA.flatMapLatestContentDataFlow { observableB }.assertError(errorC)
    }

    private suspend fun <T> Flow<Data<T>>.assertError(error: Throwable?) = test {
        val item = awaitItem()
        assertTrue(
            if (item.error is CompositeException) {
                (item.error as CompositeException).throwables == (error as CompositeException).throwables
            } else {
                item.error == error
            }
        )
        awaitComplete()
    }


    private data class TestThrowable(val name: String) : Throwable()
}