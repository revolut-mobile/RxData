package com.revolut.flowdata.extensions

import app.cash.turbine.FlowTurbine
import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest

fun <T> runFlowTest(flow: Flow<T>, validate: suspend FlowTurbine<T>.() -> Unit) = runTest {
    flow.test(validate = validate)
}