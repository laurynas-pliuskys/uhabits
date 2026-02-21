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

        var stintStart: Timestamp? = null

        for (entry in entries) {
            val isEvent = when (direction) {
                HabitDirection.NEGATIVE -> entry.value == Entry.NO
                HabitDirection.POSITIVE -> entry.value == Entry.YES_MANUAL || entry.value == Entry.YES_AUTO
            }

            if (isEvent) {
                if (stintStart != null) {
                    val stintEnd = entry.timestamp.minus(1)
                    if (stintStart <= stintEnd) {
                        list.add(Stint(stintStart, stintEnd, isActive = false))
                    }
                    stintStart = null
                }
            } else if (entry.value != Entry.UNKNOWN && entry.value != Entry.SKIP) {
                // Non-event, non-neutral: extends or starts a stint
                if (stintStart == null) stintStart = entry.timestamp
            } else {
                // UNKNOWN or SKIP: neutral — don't break stintStart but don't start one either
                // (if stintStart is already set, it remains set)
            }
        }

        // Active stint: current ongoing period with no closing event
        if (stintStart != null) {
            list.add(Stint(stintStart, to, isActive = true))
        }

        // Filter out zero-length stints (safety guard)
        list.removeAll { it.length <= 0 }
    }
}
