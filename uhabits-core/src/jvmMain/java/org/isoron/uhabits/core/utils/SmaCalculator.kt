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

import kotlin.math.max

object SmaCalculator {
    /**
     * Computes Simple Moving Average for a list of values.
     * @param values list of values in chronological order
     * @param window SMA window size (e.g., 10)
     * @return list of SMA values, same length as input.
     *         For positions with fewer than [window] prior values,
     *         uses all available values up to that point.
     */
    fun compute(values: List<Double>, window: Int): List<Double> {
        if (values.isEmpty()) return emptyList()
        return values.indices.map { i ->
            val start = max(0, i - window + 1)
            values.subList(start, i + 1).average()
        }
    }

    /**
     * Computes Exponential Moving Average for a list of values.
     * @param values list of values in chronological order
     * @param alpha smoothing factor in (0, 1]; higher = more reactive to recent values.
     *              0.3 corresponds roughly to a 5-6 period equivalent window.
     * @return list of EMA values, same length as input, seeded with the first value.
     */
    fun computeEma(values: List<Double>, alpha: Double): List<Double> {
        if (values.isEmpty()) return emptyList()
        val result = ArrayList<Double>(values.size)
        var ema = values[0]
        result.add(ema)
        for (i in 1 until values.size) {
            ema = alpha * values[i] + (1 - alpha) * ema
            result.add(ema)
        }
        return result
    }
}
