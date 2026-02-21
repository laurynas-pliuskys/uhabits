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

        // Stint between day 10 and day 4: days 9, 8, 7 (length 3, ends day before NO on day 4)
        // Active stint from day 3 to day 0 (length 3+1=? let's see: ts(3) to ts(0) = 3+1=4? Wait:
        // ts(3).daysUntil(ts(0)) = 3, so length = 3+1 = 4? No: daysAgo means days in the past.
        // ts(3) is 3 days ago, ts(0) is today.
        // daysUntil: (ts(0).unixTime - ts(3).unixTime) / DAY_LENGTH = 3
        // length = 3 + 1 = 4? That means 4 days: day-3, day-2, day-1, day-0.
        // But we only added entries for day-3 and day-2. The stint extends to `to` = ts(0).
        // Entries between day 3 and day 4 (the second NO): days 9, 8, 7 → starts day 9.
        // End = day_of_event - 1 = ts(4).minus(1) = ts(3). But ts(3) >= ts(9)? No: ts(9) is older.
        // stintStart = ts(9), stintEnd = ts(4+1-1) = ts(4).minus(1) = ts(3+? Wait:
        // event is ts(4), so stintEnd = ts(4).minus(1) which is 4-1 = ts(5)? No:
        // minus(1) subtracts 1 day. ts(4) is 4 days ago. ts(4).minus(1) = ts(4) - 1 day = 5 days ago = ts(5).
        // Hmm, but the stint started at ts(9) and ends at ts(5)? That's days 9,8,7,6,5 = 5 days.
        // But I only added entries for days 9,8,7. Days 6 and 5 are UNKNOWN (not in entries).
        // UNKNOWN is neutral - doesn't break stints. But do they start stints? No, only non-event non-UNKNOWN/SKIP.
        // stintStart gets set when we hit YES_MANUAL at ts(9). Then UNKNOWN days ts(8)? No wait:
        // I added ts(9)=YES, ts(8)=YES, ts(7)=YES and ts(4)=NO. Days 6 and 5 are UNKNOWN.
        // When we walk ts(10)..ts(0) chronologically: ts(10)=NO (event, stintStart=null stays null).
        // ts(9)=YES_MANUAL → stintStart=ts(9).
        // ts(8)=YES_MANUAL → stintStart stays ts(9).
        // ts(7)=YES_MANUAL → stintStart stays ts(9).
        // ts(6)=UNKNOWN (not in entries, so EntryList.get returns UNKNOWN) → neutral, stintStart stays ts(9).
        // ts(5)=UNKNOWN → neutral, stintStart stays ts(9).
        // ts(4)=NO (event) → stintEnd = ts(4).minus(1) = ts(5)? No: ts(4).minus(1) is ts minus 1 day.
        // ts(4) is Timestamp 4 days in the past. ts(4).minus(1) means subtract 1 day = 5 days ago = ts(5).
        // So stint is Stint(ts(9), ts(5), false). length = ts(9).daysUntil(ts(5)) + 1.
        // ts(9) is 9 days ago, ts(5) is 5 days ago. daysUntil = (newer - older) in days = 4. length = 5.
        // Wait but ts(9) is the OLDER timestamp, ts(5) is NEWER.
        // ts(9).daysUntil(ts(5)) = (ts(5).unixTime - ts(9).unixTime) / DAY_LENGTH = 4. length = 5.
        // Then ts(3)=YES, ts(2)=YES, ts(1)=UNKNOWN, ts(0)=UNKNOWN.
        // After NO at ts(4): stintStart=null.
        // ts(3)=YES → stintStart=ts(3).
        // ts(2)=YES → stintStart stays ts(3).
        // ts(1)=UNKNOWN → neutral, stintStart stays ts(3).
        // ts(0)=UNKNOWN → neutral.
        // After loop: stintStart=ts(3), active = Stint(ts(3), ts(0), true). length = 3+1=4.
        assertThat(result.size, equalTo(2))
        assertThat(result[0].start, equalTo(ts(9)))
        assertThat(result[0].end, equalTo(ts(5)))
        assertThat(result[0].length, equalTo(5))
        assertThat(result[0].isActive, equalTo(false))
        assertThat(result[1].start, equalTo(ts(3)))
        assertThat(result[1].end, equalTo(ts(0)))
        assertThat(result[1].isActive, equalTo(true))
    }

    @Test
    fun testPositive_basicStints() {
        // POSITIVE habit: event = YES_MANUAL; stints are gaps between successes
        // successes on day 10 and day 4; failures (NO) in between = the gap
        val entries = buildEntries(
            10 to Entry.YES_MANUAL,
            9 to Entry.NO,
            8 to Entry.NO,
            7 to Entry.NO,
            4 to Entry.YES_MANUAL,
            3 to Entry.NO,
            2 to Entry.NO
        )
        val stints = StintList()
        stints.recompute(entries, ts(10), ts(0), HabitDirection.POSITIVE)
        val result = stints.getAll()

        // ts(10)=YES (event), stintStart=null stays null.
        // ts(9)=NO → non-event, non-UNKNOWN/SKIP → stintStart=ts(9).
        // ts(8)=NO, ts(7)=NO → stintStart stays ts(9).
        // ts(6)=UNKNOWN, ts(5)=UNKNOWN → neutral.
        // ts(4)=YES (event) → stintEnd = ts(4).minus(1) = ts(5). Stint(ts(9), ts(5), false). length=5.
        // ts(3)=NO → stintStart=ts(3).
        // ts(2)=NO → stintStart stays ts(3).
        // ts(1)=UNKNOWN, ts(0)=UNKNOWN → neutral.
        // After loop: Stint(ts(3), ts(0), true). length=4.
        assertThat(result.size, equalTo(2))
        assertThat(result[0].start, equalTo(ts(9)))
        assertThat(result[0].end, equalTo(ts(5)))
        assertThat(result[0].length, equalTo(5))
        assertThat(result[0].isActive, equalTo(false))
        assertThat(result[1].start, equalTo(ts(3)))
        assertThat(result[1].end, equalTo(ts(0)))
        assertThat(result[1].isActive, equalTo(true))
    }

    @Test
    fun testMissingDataIsNeutral() {
        // UNKNOWN entries don't break stints
        val entries = buildEntries(
            8 to Entry.NO,
            7 to Entry.YES_MANUAL,
            6 to Entry.UNKNOWN, // neutral — should not break stint
            5 to Entry.YES_MANUAL,
            4 to Entry.NO
        )
        val stints = StintList()
        stints.recompute(entries, ts(8), ts(0), HabitDirection.NEGATIVE)
        val result = stints.getAll()

        // ts(8)=NO (event), stintStart=null.
        // ts(7)=YES → stintStart=ts(7).
        // ts(6)=UNKNOWN → neutral, stintStart stays ts(7).
        // ts(5)=YES → extends, stintStart stays ts(7).
        // ts(4)=NO (event) → stintEnd=ts(5). Stint(ts(7), ts(5), false). length=3.
        // ts(3..0) all UNKNOWN → stintStart stays null.
        // No active stint.
        assertThat(result.size, equalTo(1))
        assertThat(result[0].start, equalTo(ts(7)))
        assertThat(result[0].end, equalTo(ts(5)))
        assertThat(result[0].length, equalTo(3))
        assertThat(result[0].isActive, equalTo(false))
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

        // ts(5)=NO → stintStart=null.
        // ts(4)=NO → stintStart still null, no stint to record.
        // ts(3)=YES → stintStart=ts(3).
        // ts(2..0)=UNKNOWN → neutral.
        // Active stint: Stint(ts(3), ts(0), true). length=4.
        assertThat(result.size, equalTo(1))
        assertThat(result[0].isActive, equalTo(true))
        assertThat(result[0].start, equalTo(ts(3)))
    }

    @Test
    fun testActiveStint_extendsToToday() {
        val entries = buildEntries(
            5 to Entry.NO,
            4 to Entry.YES_MANUAL,
            3 to Entry.YES_MANUAL,
            2 to Entry.YES_MANUAL
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
    fun testNoEvents_singleActiveStint() {
        // All successes (NEGATIVE habit) → single active stint spanning entire range
        val entries = buildEntries(
            4 to Entry.YES_MANUAL,
            3 to Entry.YES_MANUAL,
            2 to Entry.YES_MANUAL,
            1 to Entry.YES_MANUAL
        )
        val stints = StintList()
        stints.recompute(entries, ts(4), ts(0), HabitDirection.NEGATIVE)
        val result = stints.getAll()

        assertThat(result.size, equalTo(1))
        assertThat(result[0].isActive, equalTo(true))
        assertThat(result[0].start, equalTo(ts(4)))
        assertThat(result[0].end, equalTo(ts(0)))
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

    @Test
    fun testYesAutoCountsAsEventForPositive() {
        // For POSITIVE direction, YES_AUTO also counts as an event
        val entries = buildEntries(
            5 to Entry.YES_AUTO, // event for POSITIVE
            4 to Entry.NO,
            3 to Entry.NO,
            2 to Entry.YES_MANUAL // event for POSITIVE
        )
        val stints = StintList()
        stints.recompute(entries, ts(5), ts(0), HabitDirection.POSITIVE)
        val result = stints.getAll()

        // ts(5)=YES_AUTO (event), stintStart=null.
        // ts(4)=NO → stintStart=ts(4).
        // ts(3)=NO → stintStart stays ts(4).
        // ts(2)=YES_MANUAL (event) → stintEnd=ts(3). Stint(ts(4), ts(3), false). length=2.
        // ts(1)=UNKNOWN, ts(0)=UNKNOWN → neutral, stintStart stays null. No active stint.
        assertThat(result.size, equalTo(1))
        assertThat(result[0].start, equalTo(ts(4)))
        assertThat(result[0].end, equalTo(ts(3)))
        assertThat(result[0].length, equalTo(2))
        assertThat(result[0].isActive, equalTo(false))
    }
}
