package com.healthexport.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.healthexport.data.healthconnect.HealthRecordType
import com.healthexport.data.healthconnect.TimeRange
import com.healthexport.data.model.ExportMode
import com.healthexport.data.model.ScheduleType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    // ── Keys ─────────────────────────────────────────────────────────────

    private object Keys {
        val SELECTED_TYPE_IDS = stringSetPreferencesKey("selected_type_ids")
        val TIME_RANGE        = stringPreferencesKey("time_range")
        val GOOGLE_ACCOUNT    = stringPreferencesKey("google_account_email")
        // Persisted after a successful export so WorkManager can re-run without UI
        val SPREADSHEET_ID    = stringPreferencesKey("spreadsheet_id")
        val EXPORT_MODE       = stringPreferencesKey("export_mode")
        val SCHEDULE_TYPE     = stringPreferencesKey("schedule_type")
    }

    // ── Selected record types ─────────────────────────────────────────────

    val selectedTypesFlow: Flow<Set<HealthRecordType>> = dataStore.data.map { prefs ->
        val ids = prefs[Keys.SELECTED_TYPE_IDS] ?: emptySet()
        ids.mapNotNull { HealthRecordType.byId(it) }.toSet()
    }

    suspend fun saveSelectedTypes(types: Set<HealthRecordType>) {
        dataStore.edit { prefs ->
            prefs[Keys.SELECTED_TYPE_IDS] = types.map { it.id }.toSet()
        }
    }

    // ── Time range ────────────────────────────────────────────────────────

    val timeRangeFlow: Flow<TimeRange> = dataStore.data.map { prefs ->
        TimeRange.deserialise(prefs[Keys.TIME_RANGE] ?: TimeRange.LastWeek.serialise())
    }

    suspend fun saveTimeRange(range: TimeRange) {
        dataStore.edit { prefs ->
            prefs[Keys.TIME_RANGE] = range.serialise()
        }
    }

    // ── Google account ────────────────────────────────────────────────────

    val googleAccountEmailFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.GOOGLE_ACCOUNT]
    }

    suspend fun saveGoogleAccountEmail(email: String?) {
        dataStore.edit { prefs ->
            if (email != null) prefs[Keys.GOOGLE_ACCOUNT] = email
            else prefs.remove(Keys.GOOGLE_ACCOUNT)
        }
    }

    // ── Export config (written after first successful export) ─────────────

    val spreadsheetIdFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.SPREADSHEET_ID]
    }

    suspend fun saveSpreadsheetId(id: String?) {
        dataStore.edit { prefs ->
            if (id != null) prefs[Keys.SPREADSHEET_ID] = id
            else prefs.remove(Keys.SPREADSHEET_ID)
        }
    }

    val exportModeFlow: Flow<ExportMode> = dataStore.data.map { prefs ->
        prefs[Keys.EXPORT_MODE]
            ?.let { runCatching { ExportMode.valueOf(it) }.getOrNull() }
            ?: ExportMode.OVERWRITE
    }

    suspend fun saveExportMode(mode: ExportMode) {
        dataStore.edit { prefs -> prefs[Keys.EXPORT_MODE] = mode.name }
    }

    val scheduleTypeFlow: Flow<ScheduleType> = dataStore.data.map { prefs ->
        prefs[Keys.SCHEDULE_TYPE]
            ?.let { runCatching { ScheduleType.valueOf(it) }.getOrNull() }
            ?: ScheduleType.ONE_SHOT
    }

    suspend fun saveScheduleType(type: ScheduleType) {
        dataStore.edit { prefs -> prefs[Keys.SCHEDULE_TYPE] = type.name }
    }
}
