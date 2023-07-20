package com.revolut.flowdata.extensions

import com.revolut.data.model.Data
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
class CombineContentKtPairTest {

    private lateinit var aFlow: MutableSharedFlow<Data<String>>
    private lateinit var bFlow: MutableSharedFlow<Data<String>>

    private lateinit var testCombinedAB: Flow<Data<String>>

    @BeforeEach
    fun setup() {
        aFlow = MutableSharedFlow()
        bFlow = MutableSharedFlow()

        testCombinedAB = combineContent(aFlow, bFlow)
            .mapData { (a, b) -> a + b }
    }

    @Test
    fun `GIVEN both data have value WHEN combineContent THEN result contains content`() = runFlowTest(testCombinedAB) {
        aFlow.emit(Data("A"))
        bFlow.emit(Data("B"))

        assertEquals(Data("AB"), expectMostRecentItem())
        expectNoEvents()
    }

    @Test
    fun `GIVEN both data loading WHEN combineContent THEN result contains loading`() = runFlowTest(testCombinedAB) {
        aFlow.emit(Data(loading = true))
        bFlow.emit(Data(loading = true))

        assertEquals(Data<String>(loading = true), expectMostRecentItem())
        expectNoEvents()
    }

    @Test
    fun `GIVEN first data has content WHEN combineContent THEN result contains no content`() = runFlowTest(testCombinedAB) {
        aFlow.emit(Data("A"))
        bFlow.emit(Data())

        assertEquals(Data<String>(), expectMostRecentItem())
        expectNoEvents()
    }

    @Test
    fun `GIVEN second data has content WHEN combineContent THEN result contains no content`() = runFlowTest(testCombinedAB) {
        aFlow.emit(Data())
        bFlow.emit(Data("B"))

        assertEquals(Data<String>(), expectMostRecentItem())
        expectNoEvents()
    }

    @Test
    fun `GIVEN second data emits later WHEN combineContent THEN first is combined when second emits`() = runFlowTest(testCombinedAB) {
        aFlow.emit(Data("A"))
        aFlow.emit(Data("B"))

        expectNoEvents()

        bFlow.emit(Data("B"))

        assertEquals(Data("BB"), expectMostRecentItem())
        expectNoEvents()
    }

    @Test
    fun `GIVEN first data emits later WHEN combineContent THEN second is combined when first emits`() = runFlowTest(testCombinedAB) {
        bFlow.emit(Data("B"))
        bFlow.emit(Data("C"))

        expectNoEvents()

        aFlow.emit(Data("A"))

        assertEquals(Data("AC"), awaitItem())
        expectNoEvents()
    }
}