package com.techrise.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.techrise.data.repository.TechRiseRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AlarmSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: TechRiseRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // 1. Only proceed if an employee/admin is logged in on this device
        val role = repository.getRole()
        if (role?.uppercase() != "ADMIN") {
            return Result.success()
        }

        // 2. Fetch all complaints
        repository.getComplaints().onSuccess { complaints ->
            val sevenDaysInSeconds = 7 * 24 * 60 * 60
            val currentSeconds = System.currentTimeMillis() / 1000

            val stagnantComplaints = complaints.filter { complaint ->
                complaint.status.uppercase() != "RESOLVED" && 
                complaint.createdAt?.let { (currentSeconds - it._seconds) > sevenDaysInSeconds } == true
            }

            // 3. Trigger alarm notifications for each stagnant complaint
            stagnantComplaints.forEach { complaint ->
                triggerEscalationAlarm(complaint.title, complaint.id)
            }
        }.onFailure {
            return Result.retry()
        }

        return Result.success()
    }

    private fun triggerEscalationAlarm(complaintTitle: String, complaintId: String) {
        val channelId = "techrise_escalation_alarms"
        val notificationId = complaintId.hashCode()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Tech Rise Escalation Alarms"
            val descriptionText = "Loud alarm warnings for 7-day stagnant complaints"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(100, 400, 100, 400, 100, 400, 500, 1000)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // High priority alarm sound
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) 
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⚠️ 7-DAY UNRESOLVED ALARM")
            .setContentText("Complaint: '$complaintTitle' is stagnant and requires immediate action!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setVibrate(longArrayOf(100, 400, 100, 400, 100, 400, 500, 1000))
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }
}
