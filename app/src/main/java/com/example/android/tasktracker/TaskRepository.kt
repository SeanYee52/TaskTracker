package com.example.android.tasktracker

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.android.tasktracker.TaskDatabase.Companion.migration_1_2
import com.example.android.tasktracker.TaskDatabase.Companion.migration_2_3
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.Year
import java.util.Calendar
import java.util.UUID

private const val DATABASE_NAME = "task-database"
private const val TAG = "TaskRepository"

class TaskRepository private constructor(
    context: Context,
    private val coroutineScope: CoroutineScope = GlobalScope
) {

    // Builds the database with the appropriate migrations for version changes.
    private val database: TaskDatabase = Room
        .databaseBuilder(
            context.applicationContext,
            TaskDatabase::class.java,
            DATABASE_NAME
        )
        .addMigrations(migration_1_2)
        .addMigrations(migration_2_3)
        .build()

    fun getTasks(): Flow<List<Task>> = database.taskDao().getTasks()

    suspend fun getTask(id: UUID): Task = database.taskDao().getTask(id)

    // Gets tasks based on the start data and end date.
    suspend fun getTasksWithUpcomingDeadlines(start: Int, end: Int): List<Task> {
        val calendar = Calendar.getInstance()

        calendar.add(Calendar.DAY_OF_YEAR, start)
        val targetStartDate = calendar.time
        val startOfDayCalendar = Calendar.getInstance()
        startOfDayCalendar.time = targetStartDate
        startOfDayCalendar.set(Calendar.HOUR_OF_DAY, 0)
        startOfDayCalendar.set(Calendar.MINUTE, 0)
        startOfDayCalendar.set(Calendar.SECOND, 0)
        startOfDayCalendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = startOfDayCalendar.time

        calendar.add(Calendar.DAY_OF_YEAR, end)
        val targetEndDate = calendar.time
        val endOfDayCalendar = Calendar.getInstance()
        endOfDayCalendar.time = targetEndDate
        endOfDayCalendar.set(Calendar.HOUR_OF_DAY, 23)
        endOfDayCalendar.set(Calendar.MINUTE, 59)
        endOfDayCalendar.set(Calendar.SECOND, 59)
        endOfDayCalendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = endOfDayCalendar.time
        return database.taskDao().getTasksWithUpcomingDeadline(startOfDay, endOfDay)
    }

    suspend fun getTasksPastDue(): List<Task> {
        val currentDate = Calendar.getInstance().time
        return database.taskDao().getTasksPastDue(currentDate)
    }

    fun updateTask(task: Task) {
        coroutineScope.launch {
            database.taskDao().updateTask(task)
        }
    }

    suspend fun addTask(task: Task){
        database.taskDao().addTask(task)
    }

    suspend fun deleteTask(task: Task){
        database.taskDao().deleteTask(task)
    }

    // Initialises repository and adds appropriate functions that can be used to call it.
    companion object {
        private var INSTANCE: TaskRepository? = null

        fun initialise(context: Context){
            if(INSTANCE == null){
                INSTANCE = TaskRepository(context)
            }
        }

        fun get(): TaskRepository{
            return INSTANCE ?:
            throw IllegalStateException("TaskRepository must be initialised")
        }
    }

}