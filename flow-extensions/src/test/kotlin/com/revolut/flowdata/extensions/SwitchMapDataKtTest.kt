package com.revolut.flowdata.extensions

import app.cash.turbine.test
import com.revolut.data.model.Data
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@ExperimentalCoroutinesApi
@RunWith(Enclosed::class)
class SwitchMapDataKtTest {


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
        fun `GIVEN A - loadingA, B - loadingB WHEN switching THEN C - loadingC`() = runTest {
            val observableA = newDataObservable(loadingA)
            val observableB = newDataObservable(loadingB)

            observableA.switchMapData { observableB }
                .assertLoading(loadingC)
        }

        private fun newDataObservable(loading: Boolean = false, error: Throwable? = null) =
            flowOf(Data("content", error, loading))


        private suspend fun <T> Flow<Data<T>>.assertLoading(loading: Boolean) = test {
            assertEquals(loading, awaitItem().loading)
            awaitComplete()
        }
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
        fun `GIVEN A - errorA, B - errorB WHEN switching THEN C - errorC`() = runTest {
            val observableA = newDataObservable(error = errorA)
            val observableB = newDataObservable(error = errorB)

            observableA.switchMapData { observableB }.assertError(errorC)
        }

        private fun newDataObservable(loading: Boolean = false, error: Throwable? = null) =
            flowOf(Data("content", error, loading))


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
}