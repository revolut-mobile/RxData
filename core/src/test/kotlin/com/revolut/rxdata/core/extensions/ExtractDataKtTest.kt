package com.revolut.rxdata.core.extensions

import com.revolut.rxdata.core.Data
import io.reactivex.rxjava3.core.Observable
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
        ).filterWhileLoading().extractContent().test().assertValues("B")
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
        ).filterWhileLoading().extractContent().test().assertValues("B")
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
        ).filterWhileLoading().extractContent().test().assertNoValues()
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
        ).filterWhileLoading().extractContent().test().assertError(error)
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
        ).filterWhileLoading().extractContent().test()
            .assertNoValues().assertError(error)
    }

    @Test
    fun `Extract data swallow errors when content present`() {
        val error = IllegalStateException()

        //Deprecated behaviour:

        Observable.just(
            Data("A", null, loading = true),
            Data("B", error, loading = false)
        ).extractData().test().assertValues("A", "B")

        //New behaviour:
        Observable.just(
            Data("A", null, loading = true),
            Data("B", error, loading = false)
        ).extractContent(consumeErrors = { _, _ -> null }).test().assertValues("A", "B")
    }

    @Test
    fun `Extract data swallow errors when content not present`() {
        val error = IllegalStateException()

        //Deprecated behaviour:

        Observable.just(
            Data("A", null, loading = true),
            Data(null, error, loading = false)
        ).extractData().test()
            .assertValues("A").assertNoErrors()

        //New behaviour:
        Observable.just(
            Data("A", null, loading = true),
            Data(null, error, loading = false)
        ).extractContent(consumeErrors = { _, _ -> null }).test()
            .assertValues("A").assertNoErrors()
    }


    @Test
    fun `extractDataOrError swallows the error if content is present`() {
        val error = IllegalStateException()

        //Deprecated behaviour:

        Observable.just(
            Data("A", null, loading = true),
            Data("B", error, loading = false)
        ).extractDataOrError().test()
            .assertValues("A", "B")

        //New behaviour:
        Observable.just(
            Data("A", null, loading = true),
            Data("B", error, loading = false)
        ).extractContent(consumeErrors = { error, content -> error.takeIf { content == null } })
            .test().assertValues("A", "B")
    }


    @Test
    fun `extractDataOrError terminates the stream if content is not present`() {
        val error = IllegalStateException()

        //Deprecated behaviour:

        Observable.just(
            Data("A", null, loading = true),
            Data(null, error, loading = false)
        ).extractDataOrError().test()
            .assertValues("A").assertError(error)

        //New behaviour:
        Observable.just(
            Data("A", null, loading = true),
            Data(null, error, loading = false)
        ).extractContent(consumeErrors = { error, content -> error.takeIf { content == null } })
            .test().assertValues("A").assertError(error)
    }


}


