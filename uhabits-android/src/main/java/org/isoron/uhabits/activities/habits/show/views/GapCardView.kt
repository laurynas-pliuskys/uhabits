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
package org.isoron.uhabits.activities.habits.show.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import org.isoron.platform.gui.toInt
import org.isoron.platform.time.JavaLocalDateFormatter
import org.isoron.uhabits.core.ui.screens.habits.show.views.GapCardState
import org.isoron.uhabits.core.ui.views.GapChart
import org.isoron.uhabits.databinding.ShowHabitGapBinding
import java.util.Locale

class GapCardView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    private val binding = ShowHabitGapBinding.inflate(LayoutInflater.from(context), this)

    fun setState(state: GapCardState) {
        val androidColor = state.theme.color(state.color).toInt()
        binding.title.setTextColor(androidColor)
        binding.gapChart.view = GapChart(state.theme, JavaLocalDateFormatter(Locale.getDefault())).apply {
            stints = state.stints
            smaValues = state.smaValues
            color = state.theme.color(state.color).withAlpha(0.6)
            activeColor = state.theme.color(state.color)
        }
        binding.gapChart.resetDataOffset()
        binding.gapChart.postInvalidate()
    }
}
