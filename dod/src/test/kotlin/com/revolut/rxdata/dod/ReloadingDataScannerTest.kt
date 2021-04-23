package com.revolut.rxdata.dod

import com.revolut.rxdata.core.Data
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

class ReloadingDataScannerTest {

    private lateinit var scanner: ReloadingDataScanner<String>

    @Before
    fun setUp() {
        scanner = ReloadingDataScanner()
    }

    @Test
    fun `Loading from DB, then reloading in background, data changing once				`(){
        newEvent(Data(null, loading = true), shouldEmit = true)
        newEvent(Data("a", loading = true), shouldEmit = true)
        newEvent(Data("a", loading = false), shouldEmit = true)

        newEvent(Data("a", loading = true), shouldEmit = false)
        newEvent(Data("a", loading = false), shouldEmit = false)

        newEvent(Data("a", loading = true), shouldEmit = false)
        newEvent(Data("a", loading = false), shouldEmit = false)

        newEvent(Data("a", loading = true), shouldEmit = false)
        newEvent(Data("a", loading = false), shouldEmit = false)
    }

    @Test
    fun `Loading from memory, then DB, then reloading in background, data changing once`(){
        newEvent(Data(null, loading = true), shouldEmit = true)
        newEvent(Data("a", loading = true), shouldEmit = true)
        newEvent(Data("a", loading = false), shouldEmit = true)

        newEvent(Data("a", loading = true), shouldEmit = false)
        newEvent(Data("a", loading = false, error = IOException("HTTP 500. All tests are green!")), shouldEmit = true)

        newEvent(Data("a", loading = true), shouldEmit = false)
        newEvent(Data("a", loading = false, error = IOException("HTTP 500. All tests are green!")), shouldEmit = false)

        newEvent(Data("a", loading = true), shouldEmit = false)
        newEvent(Data("a", loading = false, error = IOException("HTTP 500. All tests are green!")), shouldEmit = false)
    }


    @Test
    fun `Loading from memory, then DB, then reloading in background with errors`(){
        newEvent(Data("a", loading = true), shouldEmit = true)
        newEvent(Data("a", loading = false), shouldEmit = true)

        newEvent(Data("a", loading = true), shouldEmit = false)
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