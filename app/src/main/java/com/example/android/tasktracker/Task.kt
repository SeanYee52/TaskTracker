package com.example.android.tasktracker

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

// Represents the composition of a task and how its stored in the database.
@Entity
data class Task(
    @PrimaryKey val id: UUID,
    val title: String,
    val date: Date,
    val isCompleted: Boolean,
    val isImportant: Boolean,
    val inProgress: Boolean,
    val photoFileName: String? = null,
    val description: String? = null,
    val category: String = "General"
)