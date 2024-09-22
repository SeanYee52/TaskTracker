package com.example.android.tasktracker

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.Date
import java.util.UUID

@Dao
interface TaskDao{

    // Returns all tasks in the specified order.
    @Query("SELECT * FROM task ORDER BY isImportant DESC, inProgress DESC, isCompleted ASC, date ASC")
    fun getTasks(): Flow<List<Task>>

    @Query("SELECT * FROM task WHERE id=(:id)")
    suspend fun getTask(id: UUID): Task

    // Returns tasks that are between these dates and isn't completed.
    @Query("SELECT * FROM task WHERE date BETWEEN :startDate AND :endDate AND isCompleted = 0")
    suspend fun getTasksWithUpcomingDeadline(startDate: Date, endDate: Date): List<Task>

    // Returns tasks before the current date and isn't completed.
    @Query("SELECT * FROM task WHERE date < :currentDate AND isCompleted = 0")
    suspend fun getTasksPastDue(currentDate: Date): List<Task>

    @Update
    suspend fun updateTask(task: Task)

    @Insert
    suspend fun addTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)
}