package com.healthexport.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.healthexport.data.model.ScheduleType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules and cancels the periodic background export using WorkManager.
 *
 * Each call to [schedule] replaces any existing periodic work (REPLACE policy),
 * so changing the schedule interval from the wizard is handled correctly.
 *
 * Constraints applied to every request:
 * - Internet connectivity required (the Sheets API needs network).
 * - Battery must not be critically low.
 *
 * Exponential back-off is configured so transient failures (e.g. brief connectivity
 * loss) are retried automatically without hammering the API.
 */
@Singleton
class ExportScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        private const val WORK_NAME = "health_export_periodic"
    }

    /**
     * Enqueues (or replaces) a periodic export job for the given [scheduleType].
     * Calling with [ScheduleType.ONE_SHOT] is a no-op — use [cancel] to stop scheduling.
     */
    fun schedule(scheduleType: ScheduleType) {
        val (interval, unit) = when (scheduleType) {
            ScheduleType.DAILY    -> 1L  to TimeUnit.DAYS
            ScheduleType.WEEKLY   -> 7L  to TimeUnit.DAYS
            ScheduleType.MONTHLY  -> 30L to TimeUnit.DAYS
            ScheduleType.ONE_SHOT -> return   // nothing to schedule
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<ExportWorker>(interval, unit)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS,
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,   // replace interval if it changed
            request,
        )
    }

    /** Cancels any running or pending periodic export job. */
    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
