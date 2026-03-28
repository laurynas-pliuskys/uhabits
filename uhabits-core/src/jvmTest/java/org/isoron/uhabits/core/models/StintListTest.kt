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

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.isoron.uhabits.core.BaseUnitTest
import org.junit.Test

class StintListTest : BaseUnitTest() {

    // day 0 = today (FIXED_LOCAL_TIME = Jan 25, 2015)
    private fun ts(daysAgo: Int): Timestamp = timestamp(2015, 0, 25).minus(daysAgo)

    private fun buildEntries(vararg pairs: Pair<Int, Int>): EntryList {
        val entries = EntryList()
        for ((daysAgo, value) in pairs) {
            entries.add(Entry(ts(daysAgo), value))
        }
        return entries
    }

    // NEGATIVE habit: event = NO (failure); stints are sobriety streaks between failures
    @Test
    fun testNegative_basicStints() {
        // failures on day 10 and day 4; successes in between
        val entries = buildEntries(
            10 to Entry.NO,
            9 to Entry.YES_MANUAL,
            8 to Entry.YES_MANUAL,
            7 to Entry.YES_MANUAL,
            4 to Entry.NO,
            3 to Entry.YES_MANUAL,
            2 to Entry.YES_MANUAL
        )
        val stints = StintList()
        stints.recompute(entries, ts(10), ts(0), HabitDirection.NEGATIVE)
        val result = stints.getAll()

        // ts(10)=NO → null. ts(9,8,7)=YES → stintStart=ts(9).
        // ts(6)=UNKNOWN → breaks stint: Stint(ts(9), ts(7), false), length=3.
        // ts(5)=UNKNOWN → null stays null. ts(4)=NO → null stays null.
        // ts(3,2)=YES → stintStart=ts(3).
        // ts(1)=UNKNOWN → breaks stint: Stint(ts(3), ts(2), false), length=2.
        // ts(0)=UNKNOWN → null. No active stint.
        assertThat(result.size, equalTo(2))
        assertThat(result[0].start, equalTo(ts(9)))
        assertThat(result[0].end, equalTo(ts(7)))
        assertThat(result[0].length, equalTo(3))
        assertThat(result[0].isActive, equalTo(false))
        assertThat(result[1].start, equalTo(ts(3)))
        assertThat(result[1].end, equalTo(ts(2)))
        assertThat(result[1].length, equalTo(2))
        assertThat(result[1].isActive, equalTo(false))
    }

    @Test
    fun testPositive_basicStints() {
        // POSITIVE (interval) habit: each bar = gap from one YES to the next.
        // NO entries between YES events are ignored; only YES timestamps matter.
        val entries = buildEntries(
            10 to Entry.YES_MANUAL,
            9 to Entry.NO, // ignored in interval mode
            8 to Entry.NO,
            7 to Entry.NO,
            4 to Entry.YES_MANUAL,
            3 to Entry.NO,
            2 to Entry.NO
        )
        val stints = StintList()
        stints.recompute(entries, ts(10), ts(0), HabitDirection.POSITIVE)
        val result = stints.getAll()

        // Interval from ts(10) to ts(4): end=ts(5), length = ts(10).daysUntil(ts(5)) + 1 = 5+1 = 6
        // Active from ts(4) to ts(0): length = 4+1 = 5
        assertThat(result.size, equalTo(2))
        assertThat(result[0].start, equalTo(ts(10)))
        assertThat(result[0].end, equalTo(ts(5)))
        assertThat(result[0].length, equalTo(6))
        assertThat(result[0].isActive, equalTo(false))
        assertThat(result[1].start, equalTo(ts(4)))
        assertThat(result[1].end, equalTo(ts(0)))
        assertThat(result[1].isActive, equalTo(true))
        assertThat(result[1].length, equalTo(5))
    }

    @Test
    fun testMissingDataBreaksStreak() {
        // UNKNOWN (missing) entries break stints, just like NO events
        val entries = buildEntries(
            8 to Entry.NO,
            7 to Entry.YES_MANUAL,
            6 to Entry.UNKNOWN, // breaks the streak
            5 to Entry.YES_MANUAL,
            4 to Entry.NO
        )
        val stints = StintList()
        stints.recompute(entries, ts(8), ts(0), HabitDirection.NEGATIVE)
        val result = stints.getAll()

        // ts(8)=NO → null. ts(7)=YES → stintStart=ts(7).
        // ts(6)=UNKNOWN → breaks: Stint(ts(7), ts(7), false), length=1.
        // ts(5)=YES → stintStart=ts(5). ts(4)=NO → Stint(ts(5), ts(5), false), length=1.
        // ts(3..0) all UNKNOWN → null. No active stint.
        assertThat(result.size, equalTo(2))
        assertThat(result[0].start, equalTo(ts(7)))
        assertThat(result[0].end, equalTo(ts(7)))
        assertThat(result[0].length, equalTo(1))
        assertThat(result[0].isActive, equalTo(false))
        assertThat(result[1].start, equalTo(ts(5)))
        assertThat(result[1].end, equalTo(ts(5)))
        assertThat(result[1].length, equalTo(1))
        assertThat(result[1].isActive, equalTo(false))
    }

    @Test
    fun testConsecutiveEvents_noZeroLengthStint() {
        // Two consecutive failures → no stint between them
        val entries = buildEntries(
            5 to Entry.NO,
            4 to Entry.NO, // consecutive event
            3 to Entry.YES_MANUAL
        )
        val stints = StintList()
        stints.recompute(entries, ts(5), ts(0), HabitDirection.NEGATIVE)
        val result = stints.getAll()

        // ts(5)=NO → null. ts(4)=NO → null (no stint to record).
        // ts(3)=YES → stintStart=ts(3).
        // ts(2)=UNKNOWN → breaks: Stint(ts(3), ts(3), false), length=1.
        // ts(1..0)=UNKNOWN → null stays null. No active stint.
        assertThat(result.size, equalTo(1))
        assertThat(result[0].isActive, equalTo(false))
        assertThat(result[0].start, equalTo(ts(3)))
        assertThat(result[0].end, equalTo(ts(3)))
        assertThat(result[0].length, equalTo(1))
    }

    @Test
    fun testActiveStint_requiresTodayRecorded() {
        // Days 1 and 0 are unrecorded (UNKNOWN) — stint closes at last YES day
        val entries = buildEntries(
            5 to Entry.NO,
            4 to Entry.YES_MANUAL,
            3 to Entry.YES_MANUAL,
            2 to Entry.YES_MANUAL
        )
        val stints = StintList()
        stints.recompute(entries, ts(5), ts(0), HabitDirection.NEGATIVE)
        val result = stints.getAll()

        // ts(5)=NO → null. ts(4,3,2)=YES → stintStart=ts(4).
        // ts(1)=UNKNOWN → breaks: Stint(ts(4), ts(2), false), length=3. ts(0)=UNKNOWN → null.
        assertThat(result.size, equalTo(1))
        assertThat(result[0].start, equalTo(ts(4)))
        assertThat(result[0].end, equalTo(ts(2)))
        assertThat(result[0].isActive, equalTo(false))
        assertThat(result[0].length, equalTo(3))
    }

    @Test
    fun testActiveStint_whenTodayIsRecorded() {
        // Active streak only shown when today (ts(0)) is explicitly recorded
        val entries = buildEntries(
            5 to Entry.NO,
            4 to Entry.YES_MANUAL,
            3 to Entry.YES_MANUAL,
            2 to Entry.YES_MANUAL,
            1 to Entry.YES_MANUAL,
            0 to Entry.YES_MANUAL
        )
        val stints = StintList()
        stints.recompute(entries, ts(5), ts(0), HabitDirection.NEGATIVE)
        val result = stints.getAll()

        assertThat(result.size, equalTo(1))
        assertThat(result[0].start, equalTo(ts(4)))
        assertThat(result[0].end, equalTo(ts(0)))
        assertThat(result[0].isActive, equalTo(true))
        assertThat(result[0].length, equalTo(5))
    }

    @Test
    fun testNoEvents_stintClosedByUnrecordedToday() {
        // All confirmed YES days except today → stint closes at ts(1), not active
        val entries = buildEntries(
            4 to Entry.YES_MANUAL,
            3 to Entry.YES_MANUAL,
            2 to Entry.YES_MANUAL,
            1 to Entry.YES_MANUAL
        )
        val stints = StintList()
        stints.recompute(entries, ts(4), ts(0), HabitDirection.NEGATIVE)
        val result = stints.getAll()

        // ts(4,3,2,1)=YES → stintStart=ts(4). ts(0)=UNKNOWN → Stint(ts(4), ts(1), false), length=4.
        assertThat(result.size, equalTo(1))
        assertThat(result[0].isActive, equalTo(false))
        assertThat(result[0].start, equalTo(ts(4)))
        assertThat(result[0].end, equalTo(ts(1)))
        assertThat(result[0].length, equalTo(4))
    }

    @Test
    fun testAllEvents_noStints() {
        // All failures (NEGATIVE habit) → no stints
        val entries = buildEntries(
            3 to Entry.NO,
            2 to Entry.NO,
            1 to Entry.NO
        )
        val stints = StintList()
        stints.recompute(entries, ts(3), ts(0), HabitDirection.NEGATIVE)
        assertThat(stints.getAll().size, equalTo(0))
    }

    @Test
    fun testEmpty_noStints() {
        val stints = StintList()
        stints.recompute(EntryList(), ts(5), ts(0), HabitDirection.NEGATIVE)
        assertThat(stints.getAll().size, equalTo(0))
    }

    @Test
    fun testSingleEntry_event() {
        val entries = buildEntries(0 to Entry.NO)
        val stints = StintList()
        stints.recompute(entries, ts(0), ts(0), HabitDirection.NEGATIVE)
        assertThat(stints.getAll().size, equalTo(0))
    }

    @Test
    fun testSingleEntry_nonEvent() {
        val entries = buildEntries(0 to Entry.YES_MANUAL)
        val stints = StintList()
        stints.recompute(entries, ts(0), ts(0), HabitDirection.NEGATIVE)
        val result = stints.getAll()
        assertThat(result.size, equalTo(1))
        assertThat(result[0].isActive, equalTo(true))
        assertThat(result[0].length, equalTo(1))
    }

    // Real use case: interval habit (e.g., medication) where user only marks YES on dose days.
    // Days between doses are UNKNOWN (not explicitly NO). Each gap between consecutive YES
    // events should produce one bar.
    @Test
    fun testPositive_intervalWithUnknownGaps() {
        val entries = buildEntries(
            10 to Entry.YES_MANUAL, // dose on day 10
            4 to Entry.YES_MANUAL, // dose on day 4 (6-day gap from day 10)
            1 to Entry.YES_MANUAL // dose on day 1 (3-day gap from day 4)
            // days 9,8,7,6,5,3,2 are UNKNOWN (not added)
        )
        val stints = StintList()
        stints.recompute(entries, ts(10), ts(0), HabitDirection.POSITIVE)
        val result = stints.getAll()

        // Expect 3 stints:
        //   Stint(ts(10), ts(5), false): end=ts(4).minus(1)=ts(5), length=ts(10).daysUntil(ts(5))+1=5+1=6
        //   Stint(ts(4), ts(2), false): end=ts(1).minus(1)=ts(2), length=ts(4).daysUntil(ts(2))+1=2+1=3
        //   Stint(ts(1), ts(0), true): active, length = 1+1 = 2
        assertThat(result.size, equalTo(3))
        assertThat(result[0].start, equalTo(ts(10)))
        assertThat(result[0].end, equalTo(ts(5)))
        assertThat(result[0].length, equalTo(6))
        assertThat(result[0].isActive, equalTo(false))
        assertThat(result[1].start, equalTo(ts(4)))
        assertThat(result[1].end, equalTo(ts(2)))
        assertThat(result[1].length, equalTo(3))
        assertThat(result[1].isActive, equalTo(false))
        assertThat(result[2].start, equalTo(ts(1)))
        assertThat(result[2].end, equalTo(ts(0)))
        assertThat(result[2].isActive, equalTo(true))
        assertThat(result[2].length, equalTo(2))
    }

    @Test
    fun testYesAutoCountsAsEventForPositive() {
        // For POSITIVE (interval) direction, YES_AUTO also anchors an interval
        val entries = buildEntries(
            5 to Entry.YES_AUTO, // first completion
            4 to Entry.NO, // ignored in interval mode
            3 to Entry.NO,
            2 to Entry.YES_MANUAL // second completion
        )
        val stints = StintList()
        stints.recompute(entries, ts(5), ts(0), HabitDirection.POSITIVE)
        val result = stints.getAll()

        // Interval from ts(5) to ts(2): end=ts(2).minus(1)=ts(3), length=ts(5).daysUntil(ts(3))+1=2+1=3
        // Active from ts(2) to ts(0): length = 2+1 = 3
        assertThat(result.size, equalTo(2))
        assertThat(result[0].start, equalTo(ts(5)))
        assertThat(result[0].end, equalTo(ts(3)))
        assertThat(result[0].length, equalTo(3))
        assertThat(result[0].isActive, equalTo(false))
        assertThat(result[1].start, equalTo(ts(2)))
        assertThat(result[1].end, equalTo(ts(0)))
        assertThat(result[1].isActive, equalTo(true))
        assertThat(result[1].length, equalTo(3))
    }
}
