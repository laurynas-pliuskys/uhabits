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

package org.isoron.uhabits.core.ui.screens.habits.show.views

import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.models.Stint
import org.isoron.uhabits.core.ui.views.Theme
import org.isoron.uhabits.core.utils.SmaCalculator

data class GapCardState(
    val color: PaletteColor,
    val stints: List<Stint>,
    val smaValues: List<Double>,
    val theme: Theme
)

class GapCardPresenter {
    companion object {
        fun buildState(habit: Habit, theme: Theme): GapCardState {
            val stints = habit.stints.getAll()
            val durations = stints.map { it.length.toDouble() }
            val smaValues = SmaCalculator.computeEma(durations, alpha = 0.3)
            return GapCardState(
                color = habit.color,
                stints = stints,
                smaValues = smaValues,
                theme = theme
            )
        }
    }
}
