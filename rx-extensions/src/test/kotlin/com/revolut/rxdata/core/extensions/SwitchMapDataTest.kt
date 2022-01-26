package com.revolut.rxdata.core.extensions

import com.revolut.rxdata.core.CompositeException
import com.revolut.data.model.Data
import io.reactivex.Observable
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
class SwitchMapDataTest {

    @RunWith(Parameterized::class)
    class LoadingTest(
        private val loadingA: Boolean,
        private val loadingB: Boolean,
        private val loadingC: Boolean,
    ) {

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{index}: A: loading={0}, B: loading = {1}, C: loading = {2}")
            fun data() = listOf(
                arrayOf(true, true, true),
                arrayOf(true, false, true),
                arrayOf(false, true, true),
                arrayOf(false, false, false),
            )
        }

        @Test
        fun `GIVEN A - loadingA, B - loadingB WHEN switching THEN C - loadingC`() {
            val observableA = newDataObservable(loadingA)
            val observableB = newDataObservable(loadingB)

            observableA.switchMapData { observableB }.assertLoading(loadingC)
        }

        private fun newDataObservable(loading: Boolean = false, error: Throwable? = null) =
            Observable.just(Data("content", error, loading))


        private fun <T> Observable<Data<T>>.assertLoading(loading: Boolean) =
            test().assertValue { it.loading == loading }
    }


    @RunWith(Parameterized::class)
    class ErrorTest(
        private val errorA: Throwable?,
        private val errorB: Throwable?,
        private val errorC: Throwable?,
    ) {

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{index}: A: error = {0}, B: error = {1}, C: error = {2}")
            fun data() = listOf(
                arrayOf<Throwable?>(null, null, null),
                arrayOf<Throwable?>(TestThrowable("A"), null, TestThrowable("A")),
                arrayOf<Throwable?>(null, TestThrowable("B"), TestThrowable("B")),
                arrayOf<Throwable?>(
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

        @Test
        fun `GIVEN A - errorA, B - errorB WHEN switching THEN C - errorC`() {
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

        private data class TestThrowable(val name: String) : Throwable()
    }
}
