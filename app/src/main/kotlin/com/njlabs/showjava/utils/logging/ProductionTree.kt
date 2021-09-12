/*
 * Show Java - A java/apk decompiler for android
 * Copyright (c) 2019 Niranjan Rajendran
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.njlabs.showjava.utils.logging

import android.util.Log
import com.crashlytics.android.Crashlytics
import timber.log.Timber

/**
 * Logs all exceptions and anything with priority higher than [Log.WARN] to [Crashlytics] and ignores
 * the rest.
 */
class ProductionTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == Log.VERBOSE || priority == Log.DEBUG || priority == Log.INFO) {
            return
        }

        if (t !== null && t !is OutOfMemoryError && t !is StackOverflowError && t !is NoClassDefFoundError) {
            if (message.isNotEmpty()) {
                Crashlytics.log("[$tag] $message")
            }
            Crashlytics.logException(t)
            return
        }

        if (priority > Log.WARN) {
            Crashlytics.logException(Throwable("[$tag] $message"))
        }
    }
}
