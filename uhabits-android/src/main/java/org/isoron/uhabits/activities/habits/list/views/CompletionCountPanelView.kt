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

package org.isoron.uhabits.activities.habits.list.views

import android.content.Context
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.inject.ActivityContext
import javax.inject.Inject

class CompletionCountPanelViewFactory
@Inject constructor(
    @ActivityContext val context: Context,
    val preferences: Preferences,
    private val buttonFactory: CompletionCountButtonViewFactory
) {
    fun create() = CompletionCountPanelView(context, preferences, buttonFactory)
}

class CompletionCountPanelView(
    context: Context,
    preferences: Preferences,
    private val buttonFactory: CompletionCountButtonViewFactory
) : ButtonPanelView<CompletionCountButtonView>(context, preferences) {

    var completionCounts: IntArray? = null
        set(value) {
            field = value
            setupButtons()
        }

    var totalChildren: Int = 0
        set(value) {
            field = value
            setupButtons()
        }

    var color: Int = 0
        set(value) {
            field = value
            setupButtons()
        }

    override fun createButton(): CompletionCountButtonView = buttonFactory.create()

    @Synchronized
    override fun setupButtons() {
        buttons.forEachIndexed { index, button ->
            val count = if (completionCounts != null && index + dataOffset < completionCounts!!.size) {
                completionCounts!![index + dataOffset]
            } else {
                0
            }
            button.count = count
            button.total = totalChildren
            button.color = color
        }
    }
}
