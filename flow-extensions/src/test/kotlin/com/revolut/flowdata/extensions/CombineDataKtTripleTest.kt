package com.revolut.flowdata.extensions

import com.revolut.data.model.Data
import com.revolut.flow_core.extensions.runFlowTest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CombineDataKtTripleTest {

    lateinit var aSubject: MutableSharedFlow<Data<String>>
    lateinit var bSubject: MutableSharedFlow<Data<String>>
    lateinit var cSubject: MutableSharedFlow<Data<String>>

    lateinit var testCombinedABC: Flow<String>

    @Before
    fun setup() {
        aSubject = MutableSharedFlow()
        bSubject = MutableSharedFlow()
        cSubject = MutableSharedFlow()

        testCombinedABC = combineData(aSubject, bSubject, cSubject)
            .mapData { (a, b, c) -> a + b + c }
            .extractContent()
    }

    @Test
    fun `Combined values when both a and b has values`() = runFlowTest(testCombinedABC) {
        aSubject.emit(Data("A"))
        bSubject.emit(Data("B"))
        cSubject.emit(Data("C"))

        assertEquals("ABC", expectMostRecentItem())
        expectNoEvents()
    }

    @Test
    fun `No combined values while both are loading`() = runFlowTest(testCombinedABC) {
        aSubject.emit(Data(loading = true))
        bSubject.emit(Data(loading = true))
        cSubject.emit(Data(loading = true))

        expectNoEvents()
    }

    @Test
    fun `No combined values when only a has value`() = runFlowTest(testCombinedABC) {
        aSubject.emit(Data("A"))
        bSubject.emit(Data())
        cSubject.emit(Data())

        expectNoEvents()
    }

    @Test
    fun `No combined values when only b has value`() = runFlowTest(testCombinedABC) {
        aSubject.emit(Data())
        bSubject.emit(Data("B"))
        cSubject.emit(Data())

        expectNoEvents()
    }

    @Test
    fun `No combined values when only c has value`() = runFlowTest(testCombinedABC) {
        aSubject.emit(Data())
        bSubject.emit(Data())
        cSubject.emit(Data("C"))

        expectNoEvents()
    }

    @Test
    fun `Latest value from a is combined when b emits`() = runFlowTest(testCombinedABC) {
        aSubject.emit(Data("A"))
        aSubject.emit(Data("B"))

        expectNoEvents()

        bSubject.emit(Data("B"))
        cSubject.emit(Data("C"))

        assertEquals("BBC", expectMostRecentItem())
        expectNoEvents()
    }

    @Test
    fun `Latest value from b is combined when a emits`() = runFlowTest(testCombinedABC) {
        bSubject.emit(Data("B"))
        bSubject.emit(Data("C"))

        expectNoEvents()

        aSubject.emit(Data("A"))
        cSubject.emit(Data("C"))

        assertEquals("ACC", expectMostRecentItem())
        expectNoEvents()
    }

    @Test
    fun `Latest value from c is combined when a emits`() = runFlowTest(testCombinedABC) {
        cSubject.emit(Data("C"))
        cSubject.emit(Data("D"))

        expectNoEvents()

        aSubject.emit(Data("A"))
        bSubject.emit(Data("B"))

        assertEquals("ABD", expectMostRecentItem())
        expectNoEvents()
    }

}