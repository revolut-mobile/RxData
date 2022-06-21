package com.revolut.rxdata.dod

import com.revolut.data.model.Data
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.IOException

class ReloadingDataScannerTest {

    private lateinit var scanner: ReloadingDataScanner<String>

    @BeforeAll
    fun setUp() {
        scanner = ReloadingDataScanner()
    }

    @Test
    fun `Loading from DB, then reloading in background, data not changing`(){
        newEvent(Data(null, loading = true), shouldEmit = true)
        newEvent(Data("a", loading = true), shouldEmit = true)
        newEvent(Data("a", loading = false), shouldEmit = true)

        newEvent(Data("a", loading = true), shouldEmit = true)
        newEvent(Data("a", loading = false), shouldEmit = true)

        newEvent(Data("a", loading = true), shouldEmit = false)
        newEvent(Data("a", loading = false), shouldEmit = false)

        newEvent(Data("a", loading = true), shouldEmit = false)
        newEvent(Data("a", loading = false), shouldEmit = false)
    }

    @Test
    fun `Loading from DB, then reloading in background, data changing twice`(){
        newEvent(Data(null, loading = true), shouldEmit = true)
        newEvent(Data("a", loading = true), shouldEmit = true)
        newEvent(Data("a", loading = false), shouldEmit = true)

        newEvent(Data("a", loading = true), shouldEmit = true)
        newEvent(Data("b", loading = false), shouldEmit = true)

        newEvent(Data("b", loading = true), shouldEmit = true)
        newEvent(Data("b", loading = false), shouldEmit = true)

        newEvent(Data("b", loading = true), shouldEmit = true)
        newEvent(Data("b", loading = false), shouldEmit = true)

        newEvent(Data("b", loading = true), shouldEmit = false)
        newEvent(Data("b", loading = false), shouldEmit = false)

        newEvent(Data("b", loading = true), shouldEmit = false)
        newEvent(Data("c", loading = false), shouldEmit = true)

        newEvent(Data("c", loading = true), shouldEmit = true)
        newEvent(Data("c", loading = false), shouldEmit = true)

        newEvent(Data("c", loading = true), shouldEmit = true)
        newEvent(Data("c", loading = false), shouldEmit = true)

        newEvent(Data("c", loading = true), shouldEmit = false)
        newEvent(Data("c", loading = false), shouldEmit = false)
    }

    @Test
    fun `Loading from memory, then DB, then reloading in background, data changing once`(){
        newEvent(Data(null, loading = true), shouldEmit = true)
        newEvent(Data("a", loading = true), shouldEmit = true)
        newEvent(Data("a", loading = false), shouldEmit = true)

        newEvent(Data("a", loading = true), shouldEmit = true)
        newEvent(Data("a", loading = false, error = IOException("HTTP 500. All tests are green!")), shouldEmit = true)

        newEvent(Data("a", loading = true), shouldEmit = true)
        newEvent(Data("a", loading = false, error = IOException("HTTP 500. All tests are green!")), shouldEmit = true)

        newEvent(Data("a", loading = true), shouldEmit = false)
        newEvent(Data("a", loading = false, error = IOException("HTTP 500. All tests are green!")), shouldEmit = false)
    }

    @Test
    fun `Loading from memory, then DB, then reloading in background with errors`(){
        newEvent(Data("a", loading = true), shouldEmit = true)
        newEvent(Data("a", loading = false), shouldEmit = true)

        newEvent(Data("a", loading = true), shouldEmit = true)
        newEvent(Data("a", loading = false, error = IOException("HTTP 500. All tests are green!")), shouldEmit = true)

        newEvent(Data("a", loading = true), shouldEmit = true)
        newEvent(Data("a", loading = false, error = IOException("HTTP 500. All tests are green!")), shouldEmit = true)

        newEvent(Data("a", loading = true), shouldEmit = false)
        newEvent(Data("a", loading = false, error = IOException("HTTP 500. All tests are green!")), shouldEmit = false)

        newEvent(Data("a", loading = true), shouldEmit = false)
        newEvent(Data("a", loading = false, error = IOException("HTTP 500. All tests are green!")), shouldEmit = false)
    }


    private fun newEvent(data: Data<String>, shouldEmit: Boolean) {
        scanner.registerData(data)
        assertEquals(shouldEmit, scanner.shouldEmitCurrentData())
    }


}