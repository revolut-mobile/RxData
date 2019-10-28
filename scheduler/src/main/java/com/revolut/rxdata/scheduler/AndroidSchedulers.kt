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

import android.os.Handler
import android.os.Looper
import io.reactivex.Scheduler
import io.reactivex.android.plugins.RxAndroidPlugins

/** Android-specific Schedulers.  */
class AndroidSchedulers private constructor() {

    private object MainHolder {
        internal val DEFAULT: Scheduler =
            ImmediateHandlerScheduler(Handler(Looper.getMainLooper()), false)
    }

    init {
        throw AssertionError("No instances.")
    }

    companion object {

        private val MAIN_THREAD = RxAndroidPlugins.initMainThreadScheduler { MainHolder.DEFAULT }

        /** A [Scheduler] which executes actions on the Android main thread.  */
        fun mainThreadImmediate(): Scheduler = RxAndroidPlugins.onMainThreadScheduler(MAIN_THREAD)
    }

}
