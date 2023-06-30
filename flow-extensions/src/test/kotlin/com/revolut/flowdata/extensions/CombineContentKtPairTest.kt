package com.revolut.flowdata.extensions

import com.revolut.data.model.Data
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
class CombineContentKtPairTest {

    private lateinit var aFlow: MutableSharedFlow<Data<String>>
    private lateinit var bFlow: MutableSharedFlow<Data<String>>

    private lateinit var testCombinedAB: Flow<String>

    @BeforeEach
    fun setup() {
        aFlow = MutableSharedFlow()
        bFlow = MutableSharedFlow()

        testCombinedAB = combineContent(aFlow, bFlow)
            .mapData { (a, b) -> a + b }
            .extractContent()
    }

    @Test
    fun `Combined values when both a and b has values`() = runFlowTest(testCombinedAB) {
        aFlow.emit(Data("A"))
        bFlow.emit(Data("B"))

        Assertions.assertEquals("AB", expectMostRecentItem())
        expectNoEvents()
    }

    @Test
    fun `No combined values while both are loading`() = runFlowTest(testCombinedAB) {
        aFlow.emit(Data(loading = true))
        bFlow.emit(Data(loading = true))

        expectNoEvents()
    }

    @Test
    fun `No combined values when only a has value`() = runFlowTest(testCombinedAB) {
        aFlow.emit(Data("A"))
        bFlow.emit(Data())

        expectNoEvents()
    }

    @Test
    fun `No combined values when only b has value`() = runFlowTest(testCombinedAB) {
        aFlow.emit(Data())
        bFlow.emit(Data("B"))

        expectNoEvents()
    }

    @Test
    fun `Latest value from a is combined when b emits`() = runFlowTest(testCombinedAB) {
        aFlow.emit(Data("A"))
        aFlow.emit(Data("B"))

        expectNoEvents()

        bFlow.emit(Data("B"))

        Assertions.assertEquals("BB", awaitItem())
        expectNoEvents()
    }

    @Test
    fun `Latest value from b is combined when a emits`() = runFlowTest(testCombinedAB) {
        bFlow.emit(Data("B"))
        bFlow.emit(Data("C"))

        expectNoEvents()

        aFlow.emit(Data("A"))

        Assertions.assertEquals("AC", awaitItem())
        expectNoEvents()
    }
}