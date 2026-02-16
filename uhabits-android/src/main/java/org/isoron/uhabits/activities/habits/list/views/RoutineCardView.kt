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
import android.graphics.text.LineBreaker.BREAK_STRATEGY_BALANCED
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import org.isoron.platform.gui.toInt
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.ModelObservable
import org.isoron.uhabits.core.ui.screens.habits.list.ListHabitsBehavior
import org.isoron.uhabits.inject.ActivityContext
import org.isoron.uhabits.utils.currentTheme
import org.isoron.uhabits.utils.dp
import org.isoron.uhabits.utils.getFontAwesome
import org.isoron.uhabits.utils.sres
import javax.inject.Inject

class RoutineCardViewFactory
@Inject constructor(
    @ActivityContext val context: Context,
    private val completionPanelFactory: CompletionCountPanelViewFactory,
    private val behavior: ListHabitsBehavior
) {
    fun create() = RoutineCardView(context, completionPanelFactory, behavior)
}

class RoutineCardView(
    @ActivityContext context: Context,
    completionPanelFactory: CompletionCountPanelViewFactory,
    private val behavior: ListHabitsBehavior
) : FrameLayout(context),
    ModelObservable.Listener {

    var buttonCount
        get() = completionPanel.buttonCount
        set(value) {
            completionPanel.buttonCount = value
        }

    var dataOffset = 0
        set(value) {
            field = value
            completionPanel.dataOffset = value
        }

    var habit: Habit? = null
        set(newHabit) {
            if (isAttachedToWindow) {
                field?.observable?.removeListener(this)
                newHabit?.observable?.addListener(this)
            }
            field = newHabit
            if (newHabit != null) copyAttributesFrom(newHabit)
        }

    var isExpanded: Boolean = false
        set(value) {
            field = value
            updateChevron()
        }

    var completionCounts: IntArray?
        get() = completionPanel.completionCounts
        set(value) {
            completionPanel.completionCounts = value
        }

    var totalChildren: Int
        get() = completionPanel.totalChildren
        set(value) {
            completionPanel.totalChildren = value
            updateLabel()
        }

    private var completionPanel: CompletionCountPanelView
    private var innerFrame: LinearLayout
    private var label: TextView
    private var chevron: TextView

    init {
        chevron = TextView(context).apply {
            typeface = getFontAwesome()
            textSize = 15f
            gravity = Gravity.CENTER

            val size = dp(15f).toInt()
            val margin = dp(8f).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(margin, 0, margin, 0)
                gravity = Gravity.CENTER_VERTICAL
            }
        }

        label = TextView(context).apply {
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            gravity = Gravity.CENTER_VERTICAL
            if (SDK_INT >= Build.VERSION_CODES.Q) {
                breakStrategy = BREAK_STRATEGY_BALANCED
            }
        }

        completionPanel = completionPanelFactory.create()

        innerFrame = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            elevation = dp(1f)

            addView(chevron)
            addView(label)
            addView(completionPanel)

            setOnTouchListener { v, event ->
                v.background.setHotspot(event.x, event.y)
                false
            }
        }

        clipToPadding = false
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        val margin = dp(3f).toInt()
        setPadding(margin, 0, margin, margin)
        addView(innerFrame)

        updateBackground(false)
        updateChevron()
    }

    override fun onModelChange() {
        Handler(Looper.getMainLooper()).post {
            habit?.let { copyAttributesFrom(it) }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        habit?.observable?.addListener(this)
    }

    override fun onDetachedFromWindow() {
        habit?.observable?.removeListener(this)
        super.onDetachedFromWindow()
    }

    private fun copyAttributesFrom(h: Habit) {
        fun getActiveColor(habit: Habit): Int {
            return when (habit.isArchived) {
                true -> sres.getColor(R.attr.contrast60)
                false -> currentTheme().color(habit.color).toInt()
            }
        }

        val c = getActiveColor(h)
        label.setTextColor(c)
        chevron.setTextColor(c)
        completionPanel.color = c

        updateLabel()
    }

    private fun updateLabel() {
        val name = habit?.name ?: ""
        label.text = "$name ($totalChildren)"
    }

    private fun updateChevron() {
        chevron.text = if (isExpanded) {
            resources.getString(R.string.fa_chevron_down)
        } else {
            resources.getString(R.string.fa_chevron_right)
        }
    }

    private fun updateBackground(isSelected: Boolean) {
        val background = when (isSelected) {
            true -> R.drawable.selected_box
            false -> R.drawable.ripple
        }
        innerFrame.setBackgroundResource(background)
    }

    override fun setSelected(isSelected: Boolean) {
        super.setSelected(isSelected)
        updateBackground(isSelected)
    }
}
