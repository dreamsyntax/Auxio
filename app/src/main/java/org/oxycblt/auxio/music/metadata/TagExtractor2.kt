package org.oxycblt.auxio.music.metadata

import android.os.HandlerThread
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.MetadataRetriever
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.TrackGroupArray
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import org.oxycblt.auxio.music.device.RawSong
import org.oxycblt.auxio.music.fs.DeviceFile
import java.util.concurrent.Future
import javax.inject.Inject
import timber.log.Timber as L

interface TagResult {
    class Hit(val rawSong: RawSong) : TagResult
    class Miss(val file: DeviceFile) : TagResult
}

interface ExoPlayerTagExtractor {
    fun process(deviceFiles: Flow<DeviceFile>): Flow<TagResult>
}

class ExoPlayerTagExtractorImpl @Inject constructor(
    private val mediaSourceFactory: MediaSource.Factory,
    private val tagInterpreter2: TagInterpreter2,
) : ExoPlayerTagExtractor {
    override fun process(deviceFiles: Flow<DeviceFile>) = flow {
        val threadPool = ThreadPool(8, Handler(this))
        deviceFiles.collect { file ->
            threadPool.enqueue(file)
        }
        threadPool.empty()
    }

    private inner class Handler(
        private val collector: FlowCollector<TagResult>
    ) : ThreadPool.Handler<DeviceFile, TrackGroupArray> {
        override suspend fun produce(thread: HandlerThread, input: DeviceFile) =
            MetadataRetriever.retrieveMetadata(
                mediaSourceFactory,
                MediaItem.fromUri(input.uri),
                thread
            )

        override suspend fun consume(input: DeviceFile, output: TrackGroupArray) {
            if (output.isEmpty) {
                noMetadata(input)
                return
            }
            val track = output.get(0)
            if (track.length == 0) {
                noMetadata(input)
                return
            }
            val metadata = track.getFormat(0).metadata
            if (metadata == null) {
                noMetadata(input)
                return
            }
            val textTags = TextTags(metadata)
            val rawSong = RawSong(file = input)
            tagInterpreter2.interpretOn(textTags, rawSong)
            collector.emit(TagResult.Hit(rawSong))
        }

        private suspend fun noMetadata(input: DeviceFile) {
            L.e("No metadata found for $input")
            collector.emit(TagResult.Miss(input))
        }
    }
}

private class ThreadPool<I, O>(size: Int, private val handler: Handler<I, O>) {
    private val slots =
        Array<Slot<I, O>>(size) {
            Slot(
                thread = HandlerThread("Auxio:ThreadPool:$it"),
                task = null
            )
        }

    suspend fun enqueue(input: I) {
        spin@ while (true) {
            for (slot in slots) {
                val task = slot.task
                if (task == null || task.future.isDone) {
                    task?.complete()
                    slot.task = Task(input, handler.produce(slot.thread, input))
                    break@spin
                }
            }
        }
    }

    suspend fun empty() {
        spin@ while (true) {
            val slot = slots.firstOrNull { it.task != null }
            if (slot == null) {
                break@spin
            }
            val task = slot.task
            if (task != null && task.future.isDone) {
                task.complete()
                slot.task = null
            }
        }
    }

    private suspend fun Task<I, O>.complete() {
        try {
            // In-practice this should never block, as all clients
            // check if the future is done before calling this function.
            // If you don't maintain that invariant, this will explode.
            @Suppress("BlockingMethodInNonBlockingContext")
            handler.consume(input, future.get())
        } catch (e: Exception) {
            L.e("Failed to complete task for $input, ${e.stackTraceToString()}")
        }
    }

    private data class Slot<I, O>(
        val thread: HandlerThread,
        var task: Task<I, O>?
    )

    private data class Task<I, O>(
        val input: I,
        val future: Future<O>
    )

    interface Handler<I, O> {
        suspend fun produce(thread: HandlerThread, input: I): Future<O>
        suspend fun consume(input: I, output: O)
    }
}
