package com.revolut.flowdata.extensions

import java.io.PrintStream
import java.io.PrintWriter

/*
 * Copyright (C) 2020 Revolut
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

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