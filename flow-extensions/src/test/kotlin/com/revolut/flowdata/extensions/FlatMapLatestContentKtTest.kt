package com.revolut.flowdata.extensions

import app.cash.turbine.test
import com.revolut.data.model.Data
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@ExperimentalCoroutinesApi
class FlatMapLatestContentKtTest {

    @RunWith(Parameterized::class)
    class LoadingTest(
        private val loadingA: Boolean,
        private val loadingC: Boolean,
    ) {

        companion object {
            @JvmStatic
            @Parameters(name = "{index}: A: loading={0}, B: loading = {1}, C: loading = {2}")
            fun data() = listOf(
                arrayOf(true, true),
                arrayOf(false, false),
            )
        }

        @Test
        fun `GIVEN A - loadingA WHEN switching THEN C - loadingC`() = runTest {
            val observableA = newDataFlow(loadingA)
            val observableB = newFlow()

            observableA.flatMapLatestContent{ observableB }
                .assertLoading(loadingC)
        }

        private fun newDataFlow(loading: Boolean = false, error: Throwable? = null) =
            flowOf(Data("content", error, loading))

        private fun newFlow() =
            flowOf(1)

        private suspend fun <T> Flow<Data<T>>.assertLoading(loading: Boolean) = test {
            Assert.assertEquals(loading, awaitItem().loading)
            awaitComplete()
        }
    }

}