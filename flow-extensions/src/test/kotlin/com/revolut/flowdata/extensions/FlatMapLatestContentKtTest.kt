package com.revolut.flowdata.extensions

import app.cash.turbine.test
import com.revolut.data.model.Data
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

@ExperimentalCoroutinesApi
class FlatMapLatestContentKtTest {


    companion object {
        @JvmStatic
        fun data(): List<Arguments> = listOf(
            arguments(true, true),
            arguments(false, false),
        )
    }

    @ParameterizedTest
    @MethodSource("data")
    fun `GIVEN A - loadingA WHEN switching THEN C - loadingC`(
        loadingA: Boolean,
        loadingC: Boolean
    ) = runTest {
        val observableA = newDataFlow(loadingA)
        val observableB = newFlow()

        observableA.flatMapLatestContent { observableB }
            .assertLoading(loadingC)
    }

    private fun newDataFlow(loading: Boolean = false, error: Throwable? = null) =
        flowOf(Data("content", error, loading))

    private fun newFlow() =
        flowOf(1)

    private suspend fun <T> Flow<Data<T>>.assertLoading(loading: Boolean) = test {
        assertEquals(loading, awaitItem().loading)
        awaitComplete()
    }

}