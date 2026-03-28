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

package org.isoron.uhabits.core.ui.views

import org.isoron.platform.gui.Canvas
import org.isoron.platform.gui.Color
import org.isoron.platform.gui.DataView
import org.isoron.platform.gui.TextAlign
import org.isoron.platform.time.LocalDateFormatter
import org.isoron.uhabits.core.models.Stint
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.round

class GapChart(
    var theme: Theme,
    var dateFormatter: LocalDateFormatter
) : DataView {

    var stints: List<Stint> = emptyList()
    var smaValues: List<Double> = emptyList()

    // Completed stint color (lighter) and active stint color (full)
    var color: Color = Color.BLACK
    var activeColor: Color = Color.BLACK

    override var dataOffset = 0

    private val paddingTop = 20.0
    private val footerHeight = 56.0
    private val barWidth = 14.0
    private val barMargin = 4.0
    private val nGridlines = 6

    override val dataColumnWidth: Double
        get() = barWidth + barMargin * 2

    override fun draw(canvas: Canvas) {
        val width = canvas.getWidth()
        val height = canvas.getHeight()

        val columnWidth = barWidth + barMargin * 2
        val nColumns = floor(width / columnWidth).toInt()
        val marginLeft = (width - nColumns * columnWidth) / 2
        val maxBarHeight = height - footerHeight - paddingTop

        val maxLength = stints.map { it.length }.maxOrNull()?.toDouble() ?: 1.0
        val safeMaxLength = max(maxLength, 1.0)

        canvas.setColor(theme.cardBackgroundColor)
        canvas.fill()

        fun barLeft(col: Int) = marginLeft + col * columnWidth + barMargin

        fun barCenterX(col: Int) = barLeft(col) + barWidth / 2

        // Map visible column index to stints index.
        // When all stints fit on screen, left-align: oldest at col 0, empty space on the right.
        // When there are more stints than columns, right-anchor: newest at rightmost col, scrollable.
        fun stintIndex(col: Int): Int =
            if (stints.size <= nColumns) { col } else { stints.size - nColumns + col - dataOffset }

        // Draw grid lines
        canvas.setStrokeWidth(0.5)
        for (k in 1 until nGridlines) {
            val pct = 1.0 - (k.toDouble() / (nGridlines - 1))
            val y = paddingTop + maxBarHeight * pct
            canvas.setColor(theme.lowContrastTextColor)
            canvas.drawLine(0.0, y, width, y)
        }

        // Draw bars left-to-right (oldest stints on the left)
        for (col in 0 until nColumns) {
            val idx = stintIndex(col)
            if (idx < 0 || idx >= stints.size) continue
            val stint = stints[idx]
            val perc = stint.length.toDouble() / safeMaxLength
            val barHeight = round(maxBarHeight * perc)
            if (barHeight <= 0) continue

            val x = barLeft(col)
            val y = height - footerHeight - barHeight
            val barColor = if (stint.isActive) activeColor else color
            canvas.setColor(barColor)
            val r = round(barWidth * 0.15)
            if (2 * r < barHeight) {
                canvas.fillRect(x, y + r, barWidth, barHeight - r)
                canvas.fillRect(x + r, y, barWidth - 2 * r, r + 1)
                canvas.fillCircle(x + r, y + r, r)
                canvas.fillCircle(x + barWidth - r, y + r, r)
            } else {
                canvas.fillRect(x, y, barWidth, barHeight)
            }

            // Duration label above bar
            canvas.setFontSize(theme.smallTextSize)
            canvas.setTextAlign(TextAlign.CENTER)
            canvas.setColor(barColor)
            canvas.drawText(
                stint.length.toString(),
                barCenterX(col),
                y - theme.smallTextSize * 0.80
            )
        }

        // Draw SMA line
        if (smaValues.isNotEmpty()) {
            canvas.setColor(theme.mediumContrastTextColor)
            canvas.setStrokeWidth(2.0)
            var prevX = Double.NaN
            var prevY = Double.NaN
            for (col in 0 until nColumns) {
                val idx = stintIndex(col)
                if (idx < 0 || idx >= smaValues.size) continue
                val sma = smaValues[idx]
                val perc = sma / safeMaxLength
                val cx = barCenterX(col)
                val cy = height - footerHeight - perc * maxBarHeight
                if (!prevX.isNaN()) {
                    canvas.drawLine(prevX, prevY, cx, cy)
                }
                prevX = cx
                prevY = cy
            }
        }

        // Draw X-axis
        val axisY = height - footerHeight
        canvas.setColor(theme.lowContrastTextColor)
        canvas.drawLine(0.0, axisY, width, axisY)
        canvas.setColor(theme.mediumContrastTextColor)
        canvas.setTextAlign(TextAlign.CENTER)
        canvas.setFontSize(theme.smallTextSize)

        for (col in 0 until nColumns) {
            val idx = stintIndex(col)
            if (idx < 0 || idx >= stints.size) continue
            val stint = stints[idx]
            val x = barCenterX(col)
            if (stint.isActive) {
                canvas.drawText("Now", x, axisY + theme.smallTextSize * 1.0)
            } else {
                val date = stint.end.toLocalDate()
                canvas.drawText(dateFormatter.shortMonthName(date), x, axisY + theme.smallTextSize * 1.0)
                canvas.drawText(date.day.toString(), x, axisY + theme.smallTextSize * 2.4)
            }
        }
    }
}
