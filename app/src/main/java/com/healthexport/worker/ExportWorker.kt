package com.healthexport.worker

import android.content.Context
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
        const val KEY_ERROR = "error"

        // Error kinds written to output Data on failure
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
            return Result.failure(workDataOf(KEY_ERROR to ERROR_NO_CONFIG))
        }

        return try {
            exportRunner.run(
                email         = email,
                spreadsheetId = spreadsheetId,
                types         = types,
                timeRange     = timeRange,
                exportMode    = exportMode,
            )
            Result.success()
        } catch (e: UserRecoverableAuthIOException) {
            // Cannot launch an Intent from a Worker — user must re-authenticate via the app
            Result.failure(workDataOf(KEY_ERROR to ERROR_AUTH_REQUIRED))
        } catch (e: IOException) {
            // Transient network error — let WorkManager retry with exponential back-off
            Result.retry()
        } catch (e: Exception) {
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "unknown")))
        }
    }
}
