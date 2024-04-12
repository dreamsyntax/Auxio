/*
 * Copyright (c) 2024 Auxio Project
 * AuxioService.kt is part of Auxio.
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
 
package org.oxycblt.auxio

import android.annotation.SuppressLint
import android.content.Intent
import androidx.core.app.ServiceCompat
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.oxycblt.auxio.music.service.IndexingServiceFragment
import org.oxycblt.auxio.playback.service.MediaSessionServiceFragment

@AndroidEntryPoint
class AuxioService : MediaLibraryService(), ForegroundListener {
    @Inject lateinit var mediaSessionFragment: MediaSessionServiceFragment

    @Inject lateinit var indexingFragment: IndexingServiceFragment

    @SuppressLint("WrongConstant")
    override fun onCreate() {
        super.onCreate()
        mediaSessionFragment.attach(this, this)
        indexingFragment.attach(this)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        mediaSessionFragment.handleTaskRemoved()
    }

    override fun onDestroy() {
        super.onDestroy()
        indexingFragment.release()
        mediaSessionFragment.release()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession =
        mediaSessionFragment.mediaSession

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        updateForeground(ForegroundListener.Change.MEDIA_SESSION)
    }

    override fun updateForeground(change: ForegroundListener.Change) {
        if (mediaSessionFragment.hasNotification()) {
            if (change == ForegroundListener.Change.MEDIA_SESSION) {
                mediaSessionFragment.createNotification {
                    startForeground(it.notificationId, it.notification)
                }
            }
            // Nothing changed, but don't show anything music related since we can always
            // index during playback.
        } else {
            indexingFragment.createNotification {
                if (it != null) {
                    startForeground(it.code, it.build())
                } else {
                    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                }
            }
        }
    }
}

interface ForegroundListener {
    fun updateForeground(change: Change)

    enum class Change {
        MEDIA_SESSION,
        INDEXER
    }
}
