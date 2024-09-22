package com.example.android.tasktracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

const val NOTIFICATION_DEADLINE_CHANNEL_ID = "task_upcoming"
const val NOTIFICATION_PAST_DUE_CHANNEL_ID = "task_past_due"
const val NOTIFICATION_NOT_STARTED_CHANNEL_ID = "task_not_started"

class TaskTrackerApplication : Application(){
    override fun onCreate() {
        super.onCreate()
        TaskRepository.initialise(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                getSystemService(NotificationManager::class.java)

            // Deadlines upcoming
            var name = getString(R.string.notification_deadline_channel_name)
            var importance = NotificationManager.IMPORTANCE_DEFAULT
            var channel =
                NotificationChannel(NOTIFICATION_DEADLINE_CHANNEL_ID, name, importance)
            notificationManager.createNotificationChannel(channel)

            // Expired tasks
            name = getString(R.string.notification_past_due_channel_name)
            importance = NotificationManager.IMPORTANCE_HIGH
            channel =
                NotificationChannel(NOTIFICATION_PAST_DUE_CHANNEL_ID, name, importance)
            notificationManager.createNotificationChannel(channel)

            // Not started tasks
            name = getString(R.string.notification_not_started_channel_name)
            importance = NotificationManager.IMPORTANCE_LOW
            channel =
                NotificationChannel(NOTIFICATION_NOT_STARTED_CHANNEL_ID, name, importance)
            notificationManager.createNotificationChannel(channel)
        }

        scheduleDeadlineCheckWorker(this)
    }

}