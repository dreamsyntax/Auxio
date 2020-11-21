package org.oxycblt.auxio.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_state_table")
data class PlaybackState(
    @PrimaryKey(autoGenerate = true)
    var id: Long = Long.MIN_VALUE,

    @ColumnInfo(name = "song_id")
    val songId: Long = Long.MIN_VALUE,

    @ColumnInfo(name = "position")
    val position: Long,

    @ColumnInfo(name = "parent_id")
    val parentId: Long = -1L,

    @ColumnInfo(name = "mode")
    val mode: Int,

    @ColumnInfo(name = "is_shuffling")
    val isShuffling: Boolean,

    @ColumnInfo(name = "loop_mode")
    val loopMode: Int,

    @ColumnInfo(name = "in_user_queue")
    val inUserQueue: Boolean
)
