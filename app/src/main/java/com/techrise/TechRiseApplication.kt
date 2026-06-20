package com.techrise

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.techrise.data.worker.AlarmSyncWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class TechRiseApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        schedulePeriodicAlarmSync()
    }

    private fun schedulePeriodicAlarmSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<AlarmSyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "AlarmSyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }
}
