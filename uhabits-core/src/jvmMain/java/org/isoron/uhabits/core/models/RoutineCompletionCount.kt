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

/**
 * Represents the aggregated completion count for a routine (parent habit).
 *
 * This is used to display the completion status of a routine in a collapsed view,
 * showing how many child habits are completed vs the total number of children.
 *
 * @property completedCount The number of child habits that are completed for the given day
 * @property totalCount The total number of child habits
 */
data class RoutineCompletionCount(
    val completedCount: Int,
    val totalCount: Int
)
