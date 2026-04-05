package com.healthexport.data.model

/**
 * Export mode: overwrite the whole sheet or append only new rows (smart-append).
 * Stored in DataStore so WorkManager can read it without UI.
 */
enum class ExportMode { OVERWRITE, APPEND }

/**
 * How often WorkManager should re-run the export.
 * ONE_SHOT means no periodic scheduling; the others map to PeriodicWorkRequest intervals.
 * Stored in DataStore so WorkManager can read it without UI.
 */
enum class ScheduleType { ONE_SHOT, DAILY, WEEKLY, MONTHLY }
