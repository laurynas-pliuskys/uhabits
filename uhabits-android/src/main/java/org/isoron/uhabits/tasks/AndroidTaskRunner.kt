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
package org.isoron.uhabits.tasks

import android.os.Handler
import android.os.Looper
import dagger.Module
import dagger.Provides
import org.isoron.uhabits.core.AppScope
import org.isoron.uhabits.core.tasks.Task
import org.isoron.uhabits.core.tasks.TaskRunner
import java.util.HashMap
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// TODO: @Module not needed?
@Module
class AndroidTaskRunner : TaskRunner {
    private val activeTasks: LinkedList<Task> = LinkedList()
    private val taskExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val cancelled: HashMap<Task, Boolean> = HashMap()
    private val listeners: LinkedList<TaskRunner.Listener> = LinkedList<TaskRunner.Listener>()

    override fun addListener(listener: TaskRunner.Listener) {
        listeners.add(listener)
    }

    override fun execute(task: Task) {
        task.onAttached(this)
        mainHandler.post {
            if (cancelled[task] == true) return@post
            for (l in listeners) l.onTaskStarted(task)
            activeTasks.add(task)
            task.onPreExecute()
            taskExecutor.execute {
                if (cancelled[task] != true) task.doInBackground()
                mainHandler.post {
                    if (cancelled[task] != true) task.onPostExecute()
                    activeTasks.remove(task)
                    cancelled.remove(task)
                    for (l in listeners) l.onTaskFinished(task)
                }
            }
        }
    }

    override val activeTaskCount: Int
        get() = activeTasks.size

    override fun publishProgress(task: Task, progress: Int) {
        mainHandler.post { task.onProgressUpdate(progress) }
    }

    override fun removeListener(listener: TaskRunner.Listener) {
        listeners.remove(listener)
    }

    @Module
    companion object {
        @JvmStatic
        @Provides
        @AppScope
        fun provideTaskRunner(): TaskRunner {
            return AndroidTaskRunner()
        }
    }
}
