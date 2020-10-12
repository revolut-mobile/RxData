package com.revolut.rxdata.core.extensions

import com.revolut.rxdata.core.Data
import io.reactivex.Observable
import org.junit.Test

class ExtractDataKtTest {


    @Test
    fun `Loading items are skipped`() {
        //Deprecated behaviour:

        Observable.just(
            Data("A", null, loading = true),
            Data("B", null, loading = false)
        ).extractDataIfLoaded().test().assertValues("B")

        //New behaviour is the same in this case:
        Observable.just(
            Data("A", null, loading = true),
            Data("B", null, loading = false)
        ).filterWhileLoading().extractContent<String, String>().test().assertValues("B")
    }

    @Test
    fun `Loading items with errors are skipped`() {
        val error = IllegalStateException()

        //Deprecated behaviour:

        Observable.just(
            Data("A", error, loading = true),
            Data("B", null, loading = false)
        ).extractDataIfLoaded().test().assertValues("B")

        //New behaviour is the same in this case:

        Observable.just(
            Data("A", error, loading = true),
            Data("B", null, loading = false)
        ).filterWhileLoading().extractContent<String, String>().test().assertValues("B")
    }

    @Test
    fun `Error after loading`() {
        val error = IllegalStateException()

        //Deprecated behaviour:

        Observable.just(
            Data("A", null, loading = true),
            Data("A", error, loading = false)
        ).extractDataIfLoaded().test().assertValues("A")

        //New behaviour:

        Observable.just(
            Data("A", null, loading = true),
            Data("A", error, loading = false)
        ).filterWhileLoading().extractContent<String, String>().test().assertNoValues()
            .assertError(error)
    }

    @Test
    fun `Error with content is not extracted`() {
        val error = IllegalStateException()

        //Deprecated behaviour:

        Observable.just(
            Data("A", null, loading = true),
            Data("B", error, loading = false)
        ).extractDataIfLoaded().test().assertValues("B")

        //New behaviour:
        Observable.just(
            Data("A", null, loading = true),
            Data("B", error, loading = false)
        ).filterWhileLoading().extractContent<String, String>().test().assertError(error)
    }

    @Test
    fun `Error without content is extracted and terminates the stream`() {
        val error = IllegalStateException()

        //Deprecated behaviour:
        Observable.just(
            Data("A", null, loading = true),
            Data(null, error, loading = false)
        ).extractDataIfLoaded().test()
            .assertNoValues().assertNoErrors()

        //New behaviour:
        Observable.just(
            Data("A", null, loading = true),
            Data(null, error, loading = false)
        ).filterWhileLoading().extractContent<String, String>().test()
            .assertNoValues().assertError(error)
    }
}


