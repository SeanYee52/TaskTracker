package com.example.android.tasktracker

import androidx.room.TypeConverter
import java.util.Date

// Converts the data to and from the database to allow the program to work with it
class TaskTypeConverters {
    @TypeConverter
    fun fromDate(date: Date): Long{
        return date.time
    }
    @TypeConverter
    fun toDate(millisSinceEpoch: Long): Date{
        return Date(millisSinceEpoch)
    }
}