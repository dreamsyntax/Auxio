/*
 * Copyright (c) 2025 Auxio Project
 * EmbeddedCovers.kt is part of Auxio.
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
 
package org.oxycblt.musikr.covers.embedded

import java.io.ByteArrayInputStream
import org.oxycblt.musikr.covers.Cover
import org.oxycblt.musikr.covers.CoverResult
import org.oxycblt.musikr.covers.MemoryCover
import org.oxycblt.musikr.covers.MutableCovers
import org.oxycblt.musikr.fs.device.DeviceFile
import org.oxycblt.musikr.metadata.Metadata

class EmbeddedCovers(private val coverIdentifier: CoverIdentifier) : MutableCovers<MemoryCover> {
    override suspend fun obtain(id: String): CoverResult<MemoryCover> = CoverResult.Miss()

    override suspend fun create(file: DeviceFile, metadata: Metadata): CoverResult<MemoryCover> {
        val data = metadata.cover ?: return CoverResult.Miss()
        val id = coverIdentifier.identify(data)
        return CoverResult.Hit(EmbeddedCover(id, data))
    }

    override suspend fun cleanup(excluding: Collection<Cover>) {}
}

private class EmbeddedCover(override val id: String, private val data: ByteArray) : MemoryCover {
    override suspend fun open() = ByteArrayInputStream(data)

    override fun data() = data

    override fun hashCode(): Int = id.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EmbeddedCover) return false
        return id == other.id
    }
}
