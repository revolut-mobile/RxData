package com.revolut.flowdata.extensions

import app.cash.turbine.Event
import app.cash.turbine.test
import com.revolut.data.model.Data
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
class ExtractContentKtTest {

    //region default params

    @Test
    fun `Null content emits are skipped`() = runTest {
        flowOf(
            Data(null, null, loading = true),
            Data("A", null, loading = false)
        ).extractContent().test {
            assertEquals("A", expectMostRecentItem())
        }
    }

    @Test
    fun `Loading items are not skipped`() = runTest {
        flowOf(
            Data("A", null, loading = true),
            Data("B", null, loading = false)
        ).extractContent().test {
            assertEquals("A", awaitItem())
            assertEquals("B", awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `Errors with content terminate the stream`() = runTest {
        val error = IllegalStateException()

        flowOf(
            Data("A", error, loading = true)
        ).extractContent().test {
            assertEquals(error, awaitError())
        }
    }

    @Test
    fun `Errors without content terminate the stream`() = runTest {
        val error = IllegalStateException()

        flowOf(
            Data<String>(null, error, loading = true)
        ).extractContent().test {
            assertEquals(error, awaitError())
        }
    }

    //endregion

    //region consumeErrors conditionally

    @Test
    fun `Consume errors while loading`() = runTest {
        val error = IllegalStateException()

        flowOf(
            Data("A", error, loading = true)
        ).extractContent(consumeErrors = { e, content ->
            if (content != null) {
                null
            } else {
                e
            }
        }).test {
            assertEquals("A", expectMostRecentItem())
        }
    }

    @Test
    fun `Consume specific errors`() = runTest {
        val error = IllegalStateException()

        flowOf(
            Data("A", error, loading = true)
        ).extractContent(consumeErrors = { e, _ ->
            when (e) {
                is IllegalStateException -> null
                else -> e
            }
        }).test {
            assertEquals("A", expectMostRecentItem())
        }
    }

    @Test
    fun `Non-consumed errors`() = runTest {
        val error = IllegalStateException()

        flowOf(
            Data("A", error, loading = true), //error consumed, value emitted downstream
            Data("B", error, loading = true) // error not consumed, error is extracted
        ).extractContent(consumeErrors = { e, content ->
            if (e is IllegalStateException && content == "A") {
                null
            } else {
                e
            }
        }).test {
            assertEquals("A", awaitItem())
            assertEquals(error, awaitError())
        }
    }

    //endregion

    //region nullContentHandler

    @Test
    fun `Replace null content when error happened and consume that error`() = runTest {
        val error = IllegalStateException()

        flowOf<Data<String>>(
            Data(null, null, loading = true), //error consumed, value emitted downstream
            Data(null, error, loading = false) // error not consumed, error is extracted
        ).extractContent(nullContentHandler = { loading, e ->
            if (e is IllegalStateException && !loading) {
                "A"
            } else {
                null
            }
        }, consumeErrors = { e, content ->
            if (e is IllegalStateException && content == null) {
                null
            } else {
                e
            }
        }).test {
            assertEquals("A", expectMostRecentItem())
        }
    }

    @Test
    fun `Replace null with default content when loading`() = runTest {
        flowOf(
            Data(null, null, loading = true),
            Data("B", null, loading = false)
        ).extractContent(nullContentHandler = { loading, _ ->
            if (loading) {
                "A"
            } else {
                null
            }
        }).test {
            assertEquals("A", awaitItem())
            assertEquals("B", awaitItem())
            awaitComplete()
        }
    }

    //endregion

    //region Content Transformation
    @Test
    fun `contentMapper test`() = runTest {
        val knownError = IllegalStateException()
        val unknownError = IllegalArgumentException()


        flowOf(
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
        ).test {
            assertEquals(
                listOf(
                    Event.Item("Null : Loading"),           //produced by nullContentHandler since content is null
                    Event.Item("Null : Loading : Error"),   //produced by nullContentHandler since content is null
                    Event.Item("3 : Loading : Error"),      //produced by contentMapper
                    Event.Item("3 : Loading"),              //produced by contentMapper
                    Event.Item("3"),                        //produced by contentMapper
                    Event.Error(unknownError)               //non-consumed unknownError crashed the stream
                ),
                cancelAndConsumeRemainingEvents()
            )
        }
    }

    //endregion


    //region
    @Test
    fun `extractContent emit distinct content`() = runTest {
        flowOf(
            Data(null, null, loading = true),
            Data("3", null, loading = false),
            Data("3", null, loading = true),
            Data("4", null, loading = false)
        ).extractContent().test {
            assertEquals("3", awaitItem())
            assertEquals("4", awaitItem())
            awaitComplete()
        }
    }
    //endregion

}