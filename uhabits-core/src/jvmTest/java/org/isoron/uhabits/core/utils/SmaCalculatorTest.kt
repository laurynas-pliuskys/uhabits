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
package org.isoron.uhabits.core.utils

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.isoron.uhabits.core.BaseUnitTest
import org.junit.Test

class SmaCalculatorTest : BaseUnitTest() {

    private fun assertDoubleListEquals(expected: List<Double>, actual: List<Double>, delta: Double = 0.001) {
        assertThat(actual.size, equalTo(expected.size))
        for (i in expected.indices) {
            assertThat("index $i", Math.abs(actual[i] - expected[i]) < delta, equalTo(true))
        }
    }

    @Test
    fun testEmpty() {
        val result = SmaCalculator.compute(emptyList(), 10)
        assertThat(result.isEmpty(), equalTo(true))
    }

    @Test
    fun testSingleValue() {
        val result = SmaCalculator.compute(listOf(5.0), 10)
        assertDoubleListEquals(listOf(5.0), result)
    }

    @Test
    fun testWindowOne_returnsOriginal() {
        val values = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
        val result = SmaCalculator.compute(values, 1)
        assertDoubleListEquals(values, result)
    }

    @Test
    fun testWindowLargerThanData_usesAllAvailable() {
        val values = listOf(2.0, 4.0, 6.0)
        val result = SmaCalculator.compute(values, 10)
        // i=0: avg(2.0) = 2.0
        // i=1: avg(2.0, 4.0) = 3.0
        // i=2: avg(2.0, 4.0, 6.0) = 4.0
        assertDoubleListEquals(listOf(2.0, 3.0, 4.0), result)
    }

    @Test
    fun testExactWindow() {
        val values = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
        val result = SmaCalculator.compute(values, 3)
        // i=0: avg(1.0) = 1.0
        // i=1: avg(1.0, 2.0) = 1.5
        // i=2: avg(1.0, 2.0, 3.0) = 2.0
        // i=3: avg(2.0, 3.0, 4.0) = 3.0
        // i=4: avg(3.0, 4.0, 5.0) = 4.0
        assertDoubleListEquals(listOf(1.0, 1.5, 2.0, 3.0, 4.0), result)
    }

    @Test
    fun testEmaEmpty() {
        val result = SmaCalculator.computeEma(emptyList(), 0.3)
        assertThat(result.isEmpty(), equalTo(true))
    }

    @Test
    fun testEmaSingleValue() {
        val result = SmaCalculator.computeEma(listOf(10.0), 0.3)
        assertDoubleListEquals(listOf(10.0), result)
    }

    @Test
    fun testEmaSeededFromFirst() {
        // EMA is seeded with the first value; second value should blend
        val result = SmaCalculator.computeEma(listOf(10.0, 20.0), 0.3)
        // ema[0] = 10, ema[1] = 0.3*20 + 0.7*10 = 13
        assertDoubleListEquals(listOf(10.0, 13.0), result)
    }

    @Test
    fun testEmaAlphaOne_returnsOriginal() {
        // alpha=1 means EMA = current value (no smoothing)
        val values = listOf(1.0, 5.0, 3.0, 8.0)
        val result = SmaCalculator.computeEma(values, 1.0)
        assertDoubleListEquals(values, result)
    }

    @Test
    fun testEmaLargeSpikeRaisesAndDecays() {
        // After a large spike, EMA should rise then decay as small values follow
        val values = listOf(2.0, 2.0, 2.0, 30.0, 2.0, 2.0, 2.0)
        val result = SmaCalculator.computeEma(values, 0.3)
        // After spike at index 3: EMA should be clearly above 2
        assert(result[3] > 10.0) { "EMA should rise on spike: ${result[3]}" }
        // After three more 2.0 values it should be falling back
        assert(result[6] < result[3]) { "EMA should decay after spike" }
        assert(result[6] > 2.0) { "EMA should still be above baseline after 3 values" }
    }

    @Test
    fun testWindow10_standard() {
        val values = (1..15).map { it.toDouble() }
        val result = SmaCalculator.compute(values, 10)
        assertThat(result.size, equalTo(15))
        // First 9 positions use all available (expanding window)
        assertThat(Math.abs(result[0] - 1.0) < 0.001, equalTo(true)) // avg(1)
        assertThat(Math.abs(result[4] - 3.0) < 0.001, equalTo(true)) // avg(1..5)
        // Position 9 (10th element): avg(1..10) = 5.5
        assertThat(Math.abs(result[9] - 5.5) < 0.001, equalTo(true))
        // Position 14 (15th element): avg(6..15) = 10.5
        assertThat(Math.abs(result[14] - 10.5) < 0.001, equalTo(true))
    }
}
