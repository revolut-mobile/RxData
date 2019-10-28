package com.revolut.rxdata.scheduler

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.os.Message
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.plugins.RxJavaPlugins
import java.util.concurrent.TimeUnit

internal class ImmediateHandlerScheduler(private val handler: Handler, private val async: Boolean) :
    Scheduler() {

    @SuppressLint("NewApi") // Async will only be true when the API is available to call.
    override fun scheduleDirect(run: Runnable, delay: Long, unit: TimeUnit): Disposable {
        val wrappedRun = RxJavaPlugins.onSchedule(run)

        val scheduled = ScheduledRunnable(handler, wrappedRun)
        val message = Message.obtain(handler, scheduled)
        if (async) {
            message.isAsynchronous = true
        }
        handler.sendMessageDelayed(message, unit.toMillis(delay))
        return scheduled
    }

    override fun createWorker(): Scheduler.Worker {
        return HandlerWorker(handler, async)
    }

    private class HandlerWorker internal constructor(
        private val handler: Handler,
        private val async: Boolean
    ) : Scheduler.Worker() {

        @Volatile
        private var disposed: Boolean = false

        @SuppressLint("NewApi") // Async will only be true when the API is available to call.
        override fun schedule(run: Runnable, delay: Long, unit: TimeUnit): Disposable {
            if (disposed) {
                return Disposables.disposed()
            }

            val wrappedRun = RxJavaPlugins.onSchedule(run)
            val scheduled = ScheduledRunnable(handler, wrappedRun)


            val message = Message.obtain(handler, scheduled)
            message.obj = this // Used as token for batch disposal of this worker's runnables.

            if (async) {
                message.isAsynchronous = true
            }

            if (Looper.myLooper() == handler.looper && delay <= 0) {
                handler.dispatchMessage(message)
            } else {
                handler.sendMessageDelayed(message, unit.toMillis(delay))
            }

            // Re-check disposed state for removing in case we were racing a call to dispose().
            if (disposed) {
                handler.removeCallbacks(scheduled)
                return Disposables.disposed()
            }

            return scheduled
        }

        override fun dispose() {
            disposed = true
            handler.removeCallbacksAndMessages(this /* token */)
        }

        override fun isDisposed(): Boolean {
            return disposed
        }
    }

    private class ScheduledRunnable internal constructor(
        private val handler: Handler,
        private val delegate: Runnable
    ) : Runnable, Disposable {

        @Volatile
        private var disposed: Boolean = false // Tracked solely for isDisposed().

        override fun run() {
            try {
                delegate.run()
            } catch (t: Throwable) {
                RxJavaPlugins.onError(t)
            }

        }

        override fun dispose() {
            handler.removeCallbacks(this)
            disposed = true
        }

        override fun isDisposed(): Boolean {
            return disposed
        }
    }
}
