package com.example.android.tasktracker

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ Task::class ], version=3)
@TypeConverters(TaskTypeConverters::class)
abstract class TaskDatabase: RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object{
        @Volatile
        private var INSTANCE: TaskDatabase? = null

        // Add photos to tasks by attaching the filename of the photo into each entry.
        val migration_1_2 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE Task ADD COLUMN photoFileName TEXT"
                )
            }
        }

        // Add descriptions, category (to filter), and inProgress check.
        val migration_2_3 = object : Migration(3, 4){
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Task ADD COLUMN description TEXT")
                database.execSQL("ALTER TABLE Task ADD COLUMN category TEXT NOT NULL DEFAULT 'General'")
                database.execSQL("ALTER TABLE Task ADD COLUMN inProgress INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}