/*
 * Copyright (c) 2017 Auxio Project
 * BetterShuffleOrder.kt is part of Auxio.
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
 
package org.oxycblt.auxio.playback.system

import androidx.media3.common.C
import androidx.media3.exoplayer.source.ShuffleOrder
import java.util.*

/**
 * A ShuffleOrder that fixes the poorly defined default implementation of cloneAndInsert. Whereas
 * the default implementation will randomly spread out added media items, this implementation will
 * insert them in the order they are added contiguously.
 *
 * @author media3 team, Alexander Capehart (OxygenCobalt)
 */
class BetterShuffleOrder
private constructor(private val shuffled: IntArray, private val random: Random) : ShuffleOrder {
    private val indexInShuffled: IntArray = IntArray(shuffled.size)

    /**
     * Creates an instance with a specified length.
     *
     * @param length The length of the shuffle order.
     */
    constructor(length: Int) : this(length, Random())

    constructor(length: Int, random: Random) : this(createShuffledList(length, random), random)

    init {
        for (i in shuffled.indices) {
            indexInShuffled[shuffled[i]] = i
        }
    }

    override fun getLength(): Int {
        return shuffled.size
    }

    override fun getNextIndex(index: Int): Int {
        var shuffledIndex = indexInShuffled[index]
        return if (++shuffledIndex < shuffled.size) shuffled[shuffledIndex] else C.INDEX_UNSET
    }

    override fun getPreviousIndex(index: Int): Int {
        var shuffledIndex = indexInShuffled[index]
        return if (--shuffledIndex >= 0) shuffled[shuffledIndex] else C.INDEX_UNSET
    }

    override fun getLastIndex(): Int {
        return if (shuffled.isNotEmpty()) shuffled[shuffled.size - 1] else C.INDEX_UNSET
    }

    override fun getFirstIndex(): Int {
        return if (shuffled.isNotEmpty()) shuffled[0] else C.INDEX_UNSET
    }

    override fun cloneAndInsert(insertionIndex: Int, insertionCount: Int): ShuffleOrder {
        val newShuffled = IntArray(shuffled.size + insertionCount)
        val pivot = indexInShuffled[insertionIndex]
        for (i in shuffled.indices) {
            var currentIndex = shuffled[i]
            if (currentIndex > insertionIndex) {
                currentIndex += insertionCount
            }

            if (i <= pivot) {
                newShuffled[i] = currentIndex
            } else if (i > pivot) {
                newShuffled[i + insertionCount] = currentIndex
            }
        }
        for (i in 0 until insertionCount) {
            newShuffled[pivot + i + 1] = insertionIndex + i + 1
        }
        return BetterShuffleOrder(newShuffled, Random(random.nextLong()))
    }

    override fun cloneAndRemove(indexFrom: Int, indexToExclusive: Int): ShuffleOrder {
        val numberOfElementsToRemove = indexToExclusive - indexFrom
        val newShuffled = IntArray(shuffled.size - numberOfElementsToRemove)
        var foundElementsCount = 0
        for (i in shuffled.indices) {
            if (shuffled[i] in indexFrom until indexToExclusive) {
                foundElementsCount++
            } else {
                newShuffled[i - foundElementsCount] =
                    if (shuffled[i] >= indexFrom) shuffled[i] - numberOfElementsToRemove
                    else shuffled[i]
            }
        }
        return BetterShuffleOrder(newShuffled, Random(random.nextLong()))
    }

    override fun cloneAndClear(): ShuffleOrder {
        return BetterShuffleOrder(0, Random(random.nextLong()))
    }

    companion object {
        private fun createShuffledList(length: Int, random: Random): IntArray {
            val shuffled = IntArray(length)
            for (i in 0 until length) {
                val swapIndex = random.nextInt(i + 1)
                shuffled[i] = shuffled[swapIndex]
                shuffled[swapIndex] = i
            }
            return shuffled
        }
    }
}
