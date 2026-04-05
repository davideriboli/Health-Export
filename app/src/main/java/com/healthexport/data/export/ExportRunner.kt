package com.healthexport.data.export

import com.healthexport.data.healthconnect.HealthConnectRepository
import com.healthexport.data.healthconnect.HealthRecordType
import com.healthexport.data.healthconnect.TimeRange
import com.healthexport.data.model.ExportMode
import com.healthexport.data.sheets.GoogleSheetsRepository
import com.healthexport.data.sheets.RecordSchema
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/** Snapshot of progress for a single type while the export is running. */
data class ExportProgress(
    val currentType: String,
    val currentIndex: Int,
    val totalTypes: Int,
    val recordCount: Int = 0,
)

/** Final tally returned by [ExportRunner.run] on success. */
data class ExportStats(
    val totalRows: Int,
    val skippedTypes: Int,
    val spreadsheetId: String,
)

/**
 * Executes the two-phase export pipeline:
 *   1. Create all missing sheet tabs in one batchUpdate call.
 *   2. For each type: read HC records → smart-append filter → write to Sheets.
 *
 * Shared by [com.healthexport.ui.wizard.WizardViewModel] (interactive export) and
 * [com.healthexport.worker.ExportWorker] (background scheduled export).
 *
 * [onProgress] is a suspend lambda called before and after writing each type so the
 * caller can update UI or WorkManager progress. Defaults to no-op for background use.
 *
 * Throws on failure — callers are responsible for catching:
 * - [com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException]
 *   → OAuth re-authorisation required (cannot recover from a Worker).
 * - [java.io.IOException] → transient network error, suitable for WorkManager retry.
 * - [Exception] → other failure.
 */
@Singleton
class ExportRunner @Inject constructor(
    private val healthConnectRepository: HealthConnectRepository,
    private val sheetsRepository: GoogleSheetsRepository,
) {

    suspend fun run(
        email: String,
        spreadsheetId: String,
        types: List<HealthRecordType>,
        timeRange: TimeRange,
        exportMode: ExportMode,
        onProgress: suspend (ExportProgress) -> Unit = {},
    ): ExportStats {
        var totalRows    = 0
        var skippedTypes = 0

        // ── Phase 1: create all missing tabs in one API call ──────────────
        onProgress(ExportProgress(
            currentType  = "Preparazione fogli…",
            currentIndex = 0,
            totalTypes   = types.size,
        ))
        sheetsRepository.batchEnsureSheets(
            spreadsheetId = spreadsheetId,
            tabs          = types.map { it.sheetTabName to RecordSchema.forType(it).columns },
            accountEmail  = email,
        )

        // ── Phase 2: per-type read → filter → write ────────────────────────
        types.forEachIndexed { index, type ->
            onProgress(ExportProgress(
                currentType  = type.displayName,
                currentIndex = index,
                totalTypes   = types.size,
            ))

            val schema  = RecordSchema.forType(type)
            val records = healthConnectRepository.readRecordsForType(type, timeRange)
            var rows    = records.flatMap { schema.extractRows(it) }

            // Smart append: discard rows already present in the sheet
            if (exportMode == ExportMode.APPEND && rows.isNotEmpty()) {
                val lastTs = sheetsRepository.getLastTimestamp(
                    spreadsheetId, type.sheetTabName, email)
                if (lastTs != null) {
                    rows = rows.filter { row ->
                        (row.firstOrNull() as? String)?.let { it > lastTs } ?: true
                    }
                }
            }

            onProgress(ExportProgress(
                currentType  = type.displayName,
                currentIndex = index,
                totalTypes   = types.size,
                recordCount  = rows.size,
            ))

            if (rows.isEmpty()) {
                skippedTypes++
            } else {
                when (exportMode) {
                    ExportMode.OVERWRITE -> sheetsRepository.overwriteSheetData(
                        spreadsheetId, type.sheetTabName, schema.columns, rows, email)
                    ExportMode.APPEND    -> sheetsRepository.appendRows(
                        spreadsheetId, type.sheetTabName, rows, email)
                }
                totalRows += rows.size
            }

            // Courtesy delay: ~400 ms keeps us well under 60 write-req/min
            if (index < types.lastIndex) delay(400L)
        }

        return ExportStats(
            totalRows     = totalRows,
            skippedTypes  = skippedTypes,
            spreadsheetId = spreadsheetId,
        )
    }
}
