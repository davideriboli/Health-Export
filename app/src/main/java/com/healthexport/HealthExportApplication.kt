package com.healthexport

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.healthexport.worker.ExportWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HealthExportApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    /**
     * Creates the notification channel used by [ExportWorker] to report background export results.
     * Min SDK is 26 (Oreo), so NotificationChannel is always available — no SDK check needed.
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            ExportWorker.CHANNEL_ID,
            "Export in background",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Risultati degli export automatici pianificati"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
