package com.revolut.rxdata.core

import java.io.PrintStream
import java.io.PrintWriter

class CompositeException(
    val throwables: List<Throwable>
) : Exception() {

    override fun printStackTrace() {
        throwables.forEach { throwable ->
            throwable.printStackTrace()
        }
    }

    override fun printStackTrace(printStream: PrintStream?) {
        throwables.forEach { throwable ->
            throwable.printStackTrace(printStream)
        }
    }

    override fun printStackTrace(printWriter: PrintWriter?) {
        throwables.forEach { throwable ->
            throwable.printStackTrace(printWriter)
        }
    }

    override fun toString(): String =
        throwables.joinToString { throwable ->
            throwable.toString()
        }

    override val message: String? =
        throwables
            .map { throwable ->
                throwable.message
            }
            .joinToString()

}