/*
 * Copyright (c) 2024 Auxio Project
 * CoverFiles.kt is part of Auxio.
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
 
package org.oxycblt.musikr.cover

import android.content.Context
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal interface CoverFiles {
    suspend fun read(id: String): InputStream?

    suspend fun write(id: String, data: ByteArray)

    companion object {
        fun from(context: Context, path: String, format: CoverFormat): CoverFiles =
            CoverFilesImpl(File(context.filesDir, path).also { it.mkdirs() }, format)
    }
}

private class CoverFilesImpl(private val dir: File, private val coverFormat: CoverFormat) :
    CoverFiles {
    private val fileMutexes = mutableMapOf<String, Mutex>()
    private val mapMutex = Mutex()

    private suspend fun getMutexForFile(file: String): Mutex {
        return mapMutex.withLock { fileMutexes.getOrPut(file) { Mutex() } }
    }

    override suspend fun read(id: String): InputStream? =
        withContext(Dispatchers.IO) {
            try {
                File(dir, getTargetFilePath(id)).inputStream()
            } catch (e: IOException) {
                null
            }
        }

    override suspend fun write(id: String, data: ByteArray) {
        val fileMutex = getMutexForFile(id)

        fileMutex.withLock {
            val targetFile = File(dir, getTargetFilePath(id))
            if (targetFile.exists()) {
                return
            }
            withContext(Dispatchers.IO) {
                val tempFile = File(dir, getTempFilePath(id))

                try {
                    tempFile.outputStream().use { coverFormat.transcodeInto(data, it) }
                    tempFile.renameTo(targetFile)
                } catch (e: IOException) {
                    tempFile.delete()
                }
            }
        }
    }

    private fun getTargetFilePath(name: String) = "cover_${name}.${coverFormat.extension}"

    private fun getTempFilePath(name: String) = "${getTargetFilePath(name)}.tmp"
}
