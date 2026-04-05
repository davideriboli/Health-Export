package com.healthexport.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.healthexport.data.healthconnect.HealthRecordType
import com.healthexport.data.healthconnect.TimeRange
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
}
