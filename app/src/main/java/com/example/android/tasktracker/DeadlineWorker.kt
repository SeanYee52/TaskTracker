package com.example.android.tasktracker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

private const val TAG = "DeadlineWorker"

class DeadlineWorker (
    private val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker (context, workerParameters){

    override suspend fun doWork(): Result {
        Log.d(TAG, "Doing work")
        val taskRepository = TaskRepository.get()

        // Tasks that have a due date within the next two days
        val tasksWithUpcomingDeadline = taskRepository.getTasksWithUpcomingDeadlines(0, 2)
        for (task in tasksWithUpcomingDeadline) {
            if (task.isImportant || !task.inProgress) {
                val text = "The deadline for '${task.title}' is in 2 days."
                val title = "Task Deadline Reminder"
                sendNotification(context, task, text, title, NOTIFICATION_DEADLINE_CHANNEL_ID)
            }
        }

        // Tasks that are past the due date
        val tasksPastDue = taskRepository.getTasksPastDue()
        for (task in tasksPastDue) {
            val text = "'${task.title}' is already past due!"
            val title = "TASKS PAST DUE"
            sendNotification(context, task, text, title, NOTIFICATION_PAST_DUE_CHANNEL_ID)
        }

        // Tasks that have a due date within a week and have not been started
        val tasksNotStarted = taskRepository.getTasksWithUpcomingDeadlines(0, 7)
        if (tasksNotStarted.isNotEmpty()){
            val text = "There are some tasks due within a week that has not been started"
            val title = "Tasks Not Started"
            sendNotification(context, tasksNotStarted[0], text, title, NOTIFICATION_NOT_STARTED_CHANNEL_ID)
        }


        return Result.success()
    }

    private fun sendNotification(context: Context, task: Task, notificationText: String, title: String, channelID: String) {
        // Builds the notification with the relevant info
        val builder = NotificationCompat.Builder(context, channelID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        // Checks if there are permissions for notifications,
        // if there are, send the notification,
        // if not, logged the issue and don't send the notification.
        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // If permission is not granted, we can't show the notification.
                Log.e(TAG, "Permission for posting notifications not granted")
                return
            }
            notify(task.id.hashCode(), builder.build())
        }
    }

}

// Schedule the worker
fun scheduleDeadlineCheckWorker(context: Context) {
    // Sets the worker to be scheduled daily
    val workRequest = PeriodicWorkRequestBuilder<DeadlineWorker>(1, TimeUnit.DAYS)
        .setInitialDelay(1, TimeUnit.MINUTES)
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "DeadlineCheck",
        ExistingPeriodicWorkPolicy.KEEP, // Use KEEP to avoid rescheduling if already scheduled
        workRequest
    )
    Log.d(TAG, "Scheduled Work")
}