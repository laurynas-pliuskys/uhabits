/*
 * Copyright (C) 2016-2025 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.isoron.uhabits.core.models

import javax.annotation.concurrent.ThreadSafe

@ThreadSafe
class StintList {
    private val list = ArrayList<Stint>()

    @Synchronized
    fun getAll(): List<Stint> = list.toList()

    @Synchronized
    fun recompute(
        computedEntries: EntryList,
        from: Timestamp,
        to: Timestamp,
        direction: HabitDirection
    ) {
        list.clear()

        // getByInterval returns newest-first; reverse for chronological order
        val entries = computedEntries.getByInterval(from, to).reversed()

        if (direction == HabitDirection.POSITIVE) {
            // Interval mode: each bar = gap from one YES event to the next.
            var prevYes: Timestamp? = null
            for (entry in entries) {
                val isYes = entry.value == Entry.YES_MANUAL || entry.value == Entry.YES_AUTO
                if (isYes) {
                    if (prevYes != null) {
                        list.add(Stint(prevYes, entry.timestamp.minus(1), isActive = false))
                    }
                    prevYes = entry.timestamp
                }
            }
            if (prevYes != null) {
                list.add(Stint(prevYes, to, isActive = true))
            }
            list.removeAll { it.length <= 0 }
            return
        }

        // NEGATIVE direction: streak mode — measure runs of YES between NO events.
        // UNKNOWN (missing data) also breaks streaks: an unrecorded day cannot count as sobriety.
        // Only SKIP is neutral (intentionally skipped days don't end a streak).
        var stintStart: Timestamp? = null

        for (entry in entries) {
            val isEvent = entry.value == Entry.NO || entry.value == Entry.UNKNOWN

            if (isEvent) {
                if (stintStart != null) {
                    val stintEnd = entry.timestamp.minus(1)
                    if (stintStart <= stintEnd) {
                        list.add(Stint(stintStart, stintEnd, isActive = false))
                    }
                    stintStart = null
                }
            } else if (entry.value != Entry.SKIP) {
                // YES entries extend or start a stint
                if (stintStart == null) stintStart = entry.timestamp
            }
            // SKIP: neutral — stintStart unchanged
        }

        // Active stint: current ongoing period with no closing event
        if (stintStart != null) {
            list.add(Stint(stintStart, to, isActive = true))
        }

        // Filter out zero-length stints (safety guard)
        list.removeAll { it.length <= 0 }
    }
}
