package com.revolut.rxdata.core.extensions

import com.revolut.rxdata.core.Data
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test

class CombineDataKtPairTest {

    lateinit var aSubject: PublishSubject<Data<String>>
    lateinit var bSubject: PublishSubject<Data<String>>

    lateinit var testCombinedAB: TestObserver<String>

    @Before
    fun setup() {
        aSubject = PublishSubject.create()
        bSubject = PublishSubject.create()

        testCombinedAB = combineLatestData(aSubject, bSubject)
            .mapData { (a, b) -> a + b }
            .extractContent()
            .test()
    }

    @Test
    fun `Combined values when both a and b has values`() {
        aSubject.onNext(Data("A"))
        bSubject.onNext(Data("B"))

        testCombinedAB.assertValues("AB")
    }

    @Test
    fun `No combined values while both are loading`() {
        aSubject.onNext(Data(loading = true))
        bSubject.onNext(Data(loading = true))

        testCombinedAB.assertNoValues()
    }

    @Test
    fun `No combined values when only a has value`() {
        aSubject.onNext(Data("A"))
        bSubject.onNext(Data())

        testCombinedAB.assertNoValues()
    }

    @Test
    fun `No combined values when only b has value`() {
        aSubject.onNext(Data())
        bSubject.onNext(Data("B"))

        testCombinedAB.assertNoValues()
    }

    @Test
    fun `Latest value from a is combined when b emits`() {
        aSubject.onNext(Data("A"))
        aSubject.onNext(Data("B"))

        testCombinedAB.assertNoValues()

        bSubject.onNext(Data("B"))

        testCombinedAB.assertValues("BB")
    }

    @Test
    fun `Latest value from b is combined when a emits`() {
        bSubject.onNext(Data("B"))
        bSubject.onNext(Data("C"))

        testCombinedAB.assertNoValues()

        aSubject.onNext(Data("A"))

        testCombinedAB.assertValues("AC")
    }

}
