package com.revolut.rxdata.core.extensions

import com.revolut.rxdata.core.Data
import io.reactivex.Observable
import org.junit.Test

class ExtractContentKtTest {

    //region default params: filterLoading = false | strictErrors = true

    @Test
    fun `Null content emits are skipped`() {
        Observable.just(
            Data(null, null, loading = true),
            Data("A", null, loading = false)
        ).extractContent().test().assertValues("A")
    }

    @Test
    fun `Loading items are not skipped`() {
        Observable.just(
            Data("A", null, loading = true),
            Data("B", null, loading = false)
        ).extractContent(filterLoading = false).test().assertValues("A", "B")
    }

    @Test
    fun `Errors with content terminate the stream`() {
        val error = IllegalStateException()

        Observable.just(
            Data("A", error, loading = true)
        ).extractContent().test().assertError(error)
    }

    @Test
    fun `Errors without content terminate the stream`() {
        val error = IllegalStateException()

        Observable.just(
            Data(null, error, loading = true)
        ).extractContent().test().assertError(error)
    }

    //endregion

    //region filterLoading = true | strictErrors = true

    @Test
    fun `Loading items are skipped`() {
        Observable.just(
            Data("A", null, loading = true),
            Data("B", null, loading = false)
        ).extractContent(filterLoading = true).test().assertValues("B")
    }

    @Test
    fun `Loading items with errors are skipped`() {
        val error = IllegalStateException()

        Observable.just(
            Data("A", error, loading = true),
            Data("B", null, loading = false)
        ).extractContent(filterLoading = true).test().assertValues("B")
    }

    @Test
    fun `Error after loading`() {
        val error = IllegalStateException()

        Observable.just(
            Data("A", null, loading = true),
            Data("A", error, loading = false)
        ).extractContent(filterLoading = true).test().assertNoValues().assertError(error)
    }

    //endregion

    //region filterLoading = false | strictErrors = false

    @Test
    fun `Errors with content do not terminate the stream`() {
        val error = IllegalStateException()

        Observable.just(
            Data("A", error, loading = true)
        ).extractContent(strictErrors = false).test().assertValues("A")
    }

    @Test
    fun `Errors without content still terminate the stream`() {
        val error = IllegalStateException()

        Observable.just(
            Data(null, error, loading = true)
        ).extractContent(strictErrors = false).test().assertError(error)
    }

    //endregion

    //region filterLoading = true | strictErrors = false

    @Test
    fun `Error with content is not extracted`() {
        val error = IllegalStateException()

        Observable.just(
            Data("A", null, loading = true),
            Data("B", error, loading = false)
        ).extractContent(filterLoading = true, strictErrors = false).test().assertValues("B")
    }

    @Test
    fun `Error without content is extracted and terminates the stream`() {
        val error = IllegalStateException()

        Observable.just(
            Data("A", null, loading = true),
            Data(null, error, loading = false)
        ).extractContent(filterLoading = true, strictErrors = false).test()
            .assertNoValues().assertError(error)
    }

    //endregion

    //region errors consumer

    @Test
    fun `When error is consumed then downstream receives a value`() {
        val error = IllegalStateException()

        Observable.just(
            Data("A", error, loading = true)
        ).extractContent(consumeErrors = { e, _ ->
            when (e) {
                is IllegalStateException -> null
                else -> e
            }
        }).test().assertValues("A").assertNoErrors()
    }

    @Test
    fun `Conditional error consuming`() {
        val error = IllegalStateException()

        Observable.just(
            Data("A", error, loading = true), //error consumed, value emitted downstream
            Data("B", error, loading = true) // error not consumed, error is extracted
        ).extractContent(consumeErrors = { e, content ->
            if (e is IllegalStateException && content == "A") {
                null
            } else {
                e
            }
        }).test().assertValues("A").assertError(error)
    }

    //endregion

}