/*
 * Copyright (c) 2021 Auxio Project
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
 
package org.oxycblt.auxio.util

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Looper
import androidx.fragment.app.Fragment
import org.oxycblt.auxio.BuildConfig

/**
 * Shortcut for querying all items in a database and running [block] with the cursor returned. Will
 * not run if the cursor is null.
 */
fun <R> SQLiteDatabase.queryAll(tableName: String, block: (Cursor) -> R) =
    query(tableName, null, null, null, null, null, null)?.use(block)

/** Assert that we are on a background thread. */
fun assertBackgroundThread() {
    check(Looper.myLooper() != Looper.getMainLooper()) {
        "This operation must be ran on a background thread"
    }
}

/**
 * Sanitizes a nullable value that is not likely to be null. On debug builds, requireNotNull is
 * used, while on release builds, the unsafe assertion operator [!!] ]is used
 */
fun <T> unlikelyToBeNull(value: T?): T {
    return if (BuildConfig.DEBUG) {
        requireNotNull(value)
    } else {
        value!!
    }
}

/** Require the fragment is attached to an activity. */
fun Fragment.requireAttached() = check(!isDetached) { "Fragment is detached from activity" }