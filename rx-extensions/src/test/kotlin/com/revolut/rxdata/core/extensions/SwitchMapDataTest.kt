package com.revolut.rxdata.core.extensions

import com.revolut.data.model.Data
import com.revolut.rxdata.core.CompositeException
import io.reactivex.Observable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

class SwitchMapDataTest {


    companion object {

        private data class TestThrowable(val name: String) : Throwable()

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
    ) {
        val observableA = newDataObservable(loadingA)
        val observableB = newDataObservable(loadingB)

        observableA.switchMapData { observableB }.assertLoading(loadingC)
    }

    private fun <T> Observable<Data<T>>.assertLoading(loading: Boolean) =
        test().assertValue { it.loading == loading }

    @ParameterizedTest
    @MethodSource("errors")
    fun `GIVEN A - errorA, B - errorB WHEN switching THEN C - errorC`(
        errorA: Throwable?,
        errorB: Throwable?,
        errorC: Throwable?,
    ) {
        val observableA = newDataObservable(error = errorA)
        val observableB = newDataObservable(error = errorB)

        observableA.switchMapData { observableB }.assertError(errorC)
    }

    private fun newDataObservable(loading: Boolean = false, error: Throwable? = null) =
        Observable.just(Data("content", error, loading))


    private fun <T> Observable<Data<T>>.assertError(error: Throwable?) =
        test().assertValue {
            if (it.error is CompositeException) {
                (it.error as CompositeException).throwables == (error as CompositeException).throwables
            } else {
                it.error == error
            }
        }

}
