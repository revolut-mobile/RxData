package com.revolut.rxdata.core.extensions

import com.revolut.rxdata.core.Data
import io.reactivex.rxjava3.observers.TestObserver
import io.reactivex.rxjava3.subjects.PublishSubject
import org.junit.Before
import org.junit.Test

class CombineDataKtTripleTest {

    lateinit var aSubject: PublishSubject<Data<String>>
    lateinit var bSubject: PublishSubject<Data<String>>
    lateinit var cSubject: PublishSubject<Data<String>>

    lateinit var testCombinedABC: TestObserver<String>

    @Before
    fun setup() {
        aSubject = PublishSubject.create()
        bSubject = PublishSubject.create()
        cSubject = PublishSubject.create()

        testCombinedABC = combineLatestData(aSubject, bSubject, cSubject)
            .mapData { (a, b, c) -> a + b + c }
            .extractContent()
            .test()
    }

    @Test
    fun `Combined values when both a and b has values`() {
        aSubject.onNext(Data("A"))
        bSubject.onNext(Data("B"))
        cSubject.onNext(Data("C"))

        testCombinedABC.assertValues("ABC")
    }

    @Test
    fun `No combined values while both are loading`() {
        aSubject.onNext(Data(loading = true))
        bSubject.onNext(Data(loading = true))
        cSubject.onNext(Data(loading = true))

        testCombinedABC.assertNoValues()
    }

    @Test
    fun `No combined values when only a has value`() {
        aSubject.onNext(Data("A"))
        bSubject.onNext(Data())
        cSubject.onNext(Data())

        testCombinedABC.assertNoValues()
    }

    @Test
    fun `No combined values when only b has value`() {
        aSubject.onNext(Data())
        bSubject.onNext(Data("B"))
        cSubject.onNext(Data())

        testCombinedABC.assertNoValues()
    }

    @Test
    fun `No combined values when only c has value`() {
        aSubject.onNext(Data())
        bSubject.onNext(Data())
        cSubject.onNext(Data("C"))

        testCombinedABC.assertNoValues()
    }

    @Test
    fun `Latest value from a is combined when b emits`() {
        aSubject.onNext(Data("A"))
        aSubject.onNext(Data("B"))

        testCombinedABC.assertNoValues()

        bSubject.onNext(Data("B"))
        cSubject.onNext(Data("C"))

        testCombinedABC.assertValues("BBC")
    }

    @Test
    fun `Latest value from b is combined when a emits`() {
        bSubject.onNext(Data("B"))
        bSubject.onNext(Data("C"))

        testCombinedABC.assertNoValues()

        aSubject.onNext(Data("A"))
        cSubject.onNext(Data("C"))

        testCombinedABC.assertValues("ACC")
    }

    @Test
    fun `Latest value from c is combined when a emits`() {
        cSubject.onNext(Data("C"))
        cSubject.onNext(Data("D"))

        testCombinedABC.assertNoValues()

        aSubject.onNext(Data("A"))
        bSubject.onNext(Data("B"))

        testCombinedABC.assertValues("ABD")
    }

}
