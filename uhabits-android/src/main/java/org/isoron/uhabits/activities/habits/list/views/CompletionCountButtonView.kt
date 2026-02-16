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
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.view.View
import org.isoron.uhabits.R
import org.isoron.uhabits.inject.ActivityContext
import org.isoron.uhabits.utils.InterfaceUtils.getDimension
import org.isoron.uhabits.utils.dim
import javax.inject.Inject

private val BOLD_TYPEFACE = Typeface.create("sans-serif-condensed", Typeface.BOLD)

class CompletionCountButtonViewFactory
@Inject constructor(
    @ActivityContext val context: Context
) {
    fun create() = CompletionCountButtonView(context)
}

class CompletionCountButtonView(
    @ActivityContext context: Context
) : View(context) {

    var color: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    var count: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    var total: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    private val textPaint = TextPaint().apply {
        typeface = BOLD_TYPEFACE
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = dim(R.dimen.smallTextSize)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val alpha = if (total > 0 && count == total) {
            255
        } else {
            if (count == 0) 80 else 160
        }

        val displayColor = (color and 0x00FFFFFF) or (alpha shl 24)
        textPaint.color = displayColor

        val label = count.toString()
        val x = width / 2f
        val y = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f

        canvas.drawText(label, x, y, textPaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = getDimension(context, R.dimen.checkmarkWidth).toInt()
        val height = getDimension(context, R.dimen.checkmarkHeight).toInt()
        setMeasuredDimension(width, height)
    }
}
