/*
 * Copyright (c) 2024 Auxio Project
 * Indexer.kt is part of Auxio.
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
 
package org.oxycblt.auxio.music.stack

import android.net.Uri
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import org.oxycblt.auxio.music.stack.explore.Explorer
import org.oxycblt.auxio.music.stack.interpret.Interpretation
import org.oxycblt.auxio.music.stack.interpret.Interpreter
import org.oxycblt.auxio.music.stack.interpret.model.MutableLibrary

interface Indexer {
    suspend fun run(
        uris: List<Uri>,
        interpretation: Interpretation
    ): MutableLibrary
}


class IndexerImpl
@Inject
constructor(
    private val explorer: Explorer,
    private val interpreter: Interpreter
) : Indexer {
    override suspend fun run(
        uris: List<Uri>,
        interpretation: Interpretation
    ) = coroutineScope {
        val audioFiles = explorer.explore(uris).flowOn(Dispatchers.Main).buffer()
        interpreter.interpret(audioFiles, emptyFlow(), interpretation)
    }
}
