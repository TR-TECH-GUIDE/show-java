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

package com.njlabs.showjava.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.njlabs.showjava.Constants
import com.njlabs.showjava.workers.DecompilerWorker
import timber.log.Timber

/**
 * [DecompilerActionReceiver] is used to receive the cancel request from the notification action,
 * and cancel the decompilation process.
 */
class DecompilerActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Constants.WORKER.ACTION.STOP -> {
                val id = intent.getStringExtra("id")
                Timber.d("[cancel-request] ID: $id")
                id?.let {
                    context?.let {
                        DecompilerWorker.cancel(it, id)
                    }
                }
            }
            else -> {
                Timber.i("Received an unknown action.")
            }
        }
    }

}