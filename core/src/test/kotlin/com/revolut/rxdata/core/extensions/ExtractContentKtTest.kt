package com.revolut.rxdata.core.extensions

import com.revolut.rxdata.core.Data
import io.reactivex.rxjava3.core.Observable
import org.junit.Test

class ExtractContentKtTest {

    //region default params

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
        ).extractContent().test().assertValues("A", "B")
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
            Data<String>(null, error, loading = true)
        ).extractContent().test().assertError(error)
    }

    //endregion

    //region filterWhileLoading

    @Test
    fun `Loading items are skipped`() {
        Observable.just(
            Data("A", null, loading = true),
            Data("B", null, loading = false)
        ).filterWhileLoading().extractContent().test().assertValues("B")
    }

    @Test
    fun `Loading items with errors are skipped`() {
        val error = IllegalStateException()

        Observable.just(
            Data("A", error, loading = true),
            Data("B", null, loading = false)
        ).filterWhileLoading().extractContent().test().assertValues("B")
    }

    @Test
    fun `Error after loading`() {
        val error = IllegalStateException()

        Observable.just(
            Data("A", null, loading = true),
            Data("A", error, loading = false)
        ).filterWhileLoading().extractContent().test().assertNoValues()
            .assertError(error)
    }


    @Test
    fun `Error without content is extracted and terminates the stream`() {
        val error = IllegalStateException()

        Observable.just(
            Data("A", null, loading = true),
            Data(null, error, loading = false)
        ).filterWhileLoading().extractContent().test()
            .assertNoValues().assertError(error)
    }


    //endregion

    //region consumeErrors conditionally

    @Test
    fun `Consume errors while loading`() {
        val error = IllegalStateException()

        Observable.just(
            Data("A", error, loading = true)
        ).extractContent(consumeErrors = { e, content ->
            if (content != null) {
                null
            } else {
                e
            }
        }).test().assertValues("A")
    }

    @Test
    fun `Consume specific errors`() {
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
    fun `Non-consumed errors`() {
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

    //region nullContentHandler

    @Test
    fun `Replace null content when error happened and consume that error`() {
        val error = IllegalStateException()

        Observable.just<Data<String>>(
            Data(null, null, loading = true), //error consumed, value emitted downstream
            Data(null, error, loading = false) // error not consumed, error is extracted
        ).extractContent(
            nullContentHandler = { loading, e ->
                if (e is IllegalStateException && !loading) {
                    "A"
                } else {
                    null
                }
            },
            consumeErrors = { e, content ->
                if (e is IllegalStateException && content == null) {
                    null
                } else {
                    e
                }
            }).test().assertValues("A").assertNoErrors()
    }

    @Test
    fun `Replace null with default content when loading`() {
        Observable.just(
            Data(null, null, loading = true),
            Data("B", null, loading = false)
        ).extractContent(
            nullContentHandler = { loading, _ ->
                if (loading) {
                    "A"
                } else {
                    null
                }
            }).test().assertValues("A", "B").assertNoErrors()
    }

    //endregion

    //region Content Transformation
    @Test
    fun `contentMapper test`() {
        val knownError = IllegalStateException()
        val unknownError = IllegalArgumentException()


        Observable.just(
            Data(null, null, loading = true),       // Null content Loading
            Data(null, knownError, loading = true), // Known Error while null content
            Data(3, knownError, loading = true),    // Known Error with content
            Data(3, null, loading = true),          // Content Loading
            Data(3, null, loading = false),         // Loaded Data
            Data(3, unknownError, loading = false)  // Error Happened
        ).extractContent(
            consumeErrors = { error, content ->
                error.takeUnless { it == knownError }
            },
            contentMapper = { content, loading, consumedError ->
                listOfNotNull(
                    content,
                    "Loading".takeIf { loading },
                    "Error".takeIf { consumedError != null }).joinToString(separator = " : ")
            },
            nullContentHandler = { loading, consumedError ->
                listOfNotNull(
                    "Null",
                    "Loading".takeIf { loading },
                    "Error".takeIf { consumedError != null }).joinToString(separator = " : ")
            }
        ).test()
            .assertValues(
                "Null : Loading",           //produced by nullContentHandler since content is null
                "Null : Loading : Error",   //produced by nullContentHandler since content is null
                "3 : Loading : Error",      //produced by contentMapper
                "3 : Loading",              //produced by contentMapper
                "3"                         //produced by contentMapper
            )
            .assertError(unknownError)      //non-consumed unknownError crashed the stream
    }

    //endregion

}