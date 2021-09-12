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

package com.njlabs.showjava.utils.streams

import androidx.annotation.NonNull
import timber.log.Timber
import java.io.OutputStream
import java.nio.charset.Charset


/**
 * A custom output stream that strips unnecessary stuff from raw input stream
 */
class ProgressStream(private val sendStatus: (log: String) -> Unit) : OutputStream() {

    private val validProgressRegex = Regex("^[^.][a-zA-Z/\$;\\s0-9.]+\$")

    private fun shouldIgnore(string: String): Boolean {
        if (string.startsWith("[ignored]")) {
            return true
        }
        for (part in arrayOf(
            "TRYBLOCK", "stack info", "Produces", "ASTORE", "targets",
            "WARN jadx", "thread-1", "ERROR jadx", "JadxRuntimeException",
            "java.lang"
        )) {
            if (string.contains(part, true)) {
                return true
            }
        }

        return !validProgressRegex.matches(string)
    }

    override fun write(@NonNull data: ByteArray, offset: Int, length: Int) {
        var str = String(
            data.copyOfRange(offset, length),
            Charset.forName("UTF-8")
        )
            .replace("\n", "")
            .replace("\r", "")
            .replace("INFO:".toRegex(RegexOption.IGNORE_CASE), "")
            .replace("ERROR:".toRegex(RegexOption.IGNORE_CASE), "")
            .replace("WARN:".toRegex(RegexOption.IGNORE_CASE), "")
            .replace("\n\r", "")
            .replace("... done", "")
            .replace("at", "")
            .replace("Processing ".toRegex(RegexOption.IGNORE_CASE), "")
            .replace("Decompiling ".toRegex(RegexOption.IGNORE_CASE), "")
            .replace("Extracting ".toRegex(RegexOption.IGNORE_CASE), "")
            .trim()

        if (shouldIgnore(str)) {
            return
        }

        if (str.startsWith("[stdout]")) {
            str = str.removePrefix("[stdout] ")
        }

        if (str.isNotEmpty()) {
            Timber.d("[stdout] %s", str)
            sendStatus(str)
        }
    }

    override fun write(byte: Int) {
        // Just a stub. We aren't implementing this.
    }
}