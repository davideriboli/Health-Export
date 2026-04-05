package com.healthexport.worker

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.healthexport.data.export.ExportRunner
import com.healthexport.data.preferences.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.io.IOException

/**
 * Background worker that re-runs the export using the configuration persisted in DataStore
 * after the last successful manual export.
 *
 * Result semantics:
 * - [Result.success]  — export completed (possibly with skipped types).
 * - [Result.retry]    — transient network error; WorkManager will retry with back-off.
 * - [Result.failure]  — permanent failure (auth required or bad config). The error kind
 *   is stored in output data under [KEY_ERROR] so callers can inspect it.
 *
 * A result notification is posted to [CHANNEL_ID] (created in HealthExportApplication).
 * On Android 13+, the notification is silently skipped if [POST_NOTIFICATIONS] was not granted.
 *
 * Note: OAuth re-authorisation ([UserRecoverableAuthIOException]) cannot be handled from a
 * background Worker — the user must re-open the app and re-export manually to refresh tokens.
 */
@HiltWorker
class ExportWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val exportRunner: ExportRunner,
    private val preferencesRepository: UserPreferencesRepository,
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID      = "health_export_results"
        const val NOTIFICATION_ID = 1001

        const val KEY_ERROR           = "error"
        const val ERROR_AUTH_REQUIRED = "auth_required"
        const val ERROR_NO_CONFIG     = "no_config"
    }

    override suspend fun doWork(): Result {
        val email         = preferencesRepository.googleAccountEmailFlow.first()
        val spreadsheetId = preferencesRepository.spreadsheetIdFlow.first()
        val types         = preferencesRepository.selectedTypesFlow.first().toList()
        val timeRange     = preferencesRepository.timeRangeFlow.first()
        val exportMode    = preferencesRepository.exportModeFlow.first()

        if (email == null || spreadsheetId == null || types.isEmpty()) {
            postNotification(
                title = "Export fallito",
                text  = "Configura di nuovo l'export dall'app.",
            )
            return Result.failure(workDataOf(KEY_ERROR to ERROR_NO_CONFIG))
        }

        return try {
            val stats = exportRunner.run(
                email         = email,
                spreadsheetId = spreadsheetId,
                types         = types,
                timeRange     = timeRange,
                exportMode    = exportMode,
            )
            postNotification(
                title = "Export completato",
                text  = "${stats.totalRows} righe esportate" +
                        if (stats.skippedTypes > 0) " · ${stats.skippedTypes} tipi saltati" else "",
            )
            Result.success()
        } catch (e: UserRecoverableAuthIOException) {
            // Cannot launch an Intent from a Worker — user must re-authenticate via the app
            postNotification(
                title = "Export fallito — autorizzazione scaduta",
                text  = "Apri l'app e ri-effettua il login con Google.",
            )
            Result.failure(workDataOf(KEY_ERROR to ERROR_AUTH_REQUIRED))
        } catch (e: IOException) {
            // Transient network error — let WorkManager retry with exponential back-off
            Result.retry()
        } catch (e: Exception) {
            postNotification(
                title = "Export fallito",
                text  = e.message ?: "Errore sconosciuto.",
            )
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "unknown")))
        }
    }

    /**
     * Posts a result notification on the [CHANNEL_ID] channel.
     * Wraps [SecurityException] so the Worker doesn't crash if [POST_NOTIFICATIONS] was denied.
     */
    private fun postNotification(title: String, text: String) {
        try {
            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_sync)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted on Android 13+ — silently skip
        }
    }
}
