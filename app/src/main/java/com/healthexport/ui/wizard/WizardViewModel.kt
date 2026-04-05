package com.healthexport.ui.wizard

import android.content.Intent
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.healthexport.data.google.GoogleAuthManager
import com.healthexport.data.google.GoogleSignInResult
import com.healthexport.data.healthconnect.HealthConnectRepository
import com.healthexport.data.healthconnect.HealthRecordType
import com.healthexport.data.healthconnect.RecordCategory
import com.healthexport.data.healthconnect.TimeRange
import com.healthexport.data.preferences.UserPreferencesRepository
import com.healthexport.data.sheets.GoogleSheetsRepository
import com.healthexport.data.sheets.RecordSchema
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Enums ─────────────────────────────────────────────────────────────────────

enum class SdkStatus       { CHECKING, AVAILABLE, UNAVAILABLE, UPDATE_REQUIRED }
enum class SpreadsheetMode { CREATE_NEW, USE_EXISTING }
enum class ExportMode      { OVERWRITE, APPEND }
enum class ScheduleType    { ONE_SHOT, DAILY, WEEKLY, MONTHLY }

// ── Export state ──────────────────────────────────────────────────────────────

sealed class ExportState {
    data object Idle : ExportState()

    data class InProgress(
        val currentType: String  = "",
        val currentIndex: Int    = 0,
        val totalTypes: Int      = 0,
        val recordCount: Int     = 0,   // records found for current type
    ) : ExportState() {
        val progress: Float
            get() = if (totalTypes > 0) currentIndex.toFloat() / totalTypes else 0f
    }

    data class Success(
        val totalRows: Int,
        val skippedTypes: Int,
        val spreadsheetId: String,
    ) : ExportState()

    data class Error(val message: String) : ExportState()
}

// ── One-shot events ───────────────────────────────────────────────────────────

sealed class WizardEvent {
    data class SheetsAuthRequired(val intent: Intent) : WizardEvent()
}

// ── UI state ──────────────────────────────────────────────────────────────────

data class WizardUiState(
    // Step 1
    val sdkStatus: SdkStatus                 = SdkStatus.CHECKING,
    val grantedPermissions: Set<String>      = emptySet(),
    val selectedTypes: Set<HealthRecordType> = emptySet(),
    // Step 2
    val timeRange: TimeRange                 = TimeRange.LastWeek,
    // Step 3
    val googleAccountEmail: String?          = null,
    val googleAccountName: String?           = null,
    val spreadsheetMode: SpreadsheetMode     = SpreadsheetMode.CREATE_NEW,
    val spreadsheetId: String?               = null,
    val spreadsheetName: String              = "HealthExport",
    val exportMode: ExportMode               = ExportMode.OVERWRITE,
    val scheduleType: ScheduleType           = ScheduleType.ONE_SHOT,
    // Step 4
    val exportState: ExportState             = ExportState.Idle,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class WizardViewModel @Inject constructor(
    private val healthConnectRepository: HealthConnectRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val sheetsRepository: GoogleSheetsRepository,
    val authManager: GoogleAuthManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WizardUiState())
    val uiState: StateFlow<WizardUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<WizardEvent>()
    val events: SharedFlow<WizardEvent> = _events.asSharedFlow()

    init {
        val sdkStatus = when (healthConnectRepository.sdkStatus) {
            HealthConnectClient.SDK_AVAILABLE                            -> SdkStatus.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> SdkStatus.UPDATE_REQUIRED
            else                                                         -> SdkStatus.UNAVAILABLE
        }
        _uiState.update { it.copy(sdkStatus = sdkStatus) }

        viewModelScope.launch {
            combine(
                preferencesRepository.selectedTypesFlow,
                preferencesRepository.timeRangeFlow,
                preferencesRepository.googleAccountEmailFlow,
            ) { types, range, email -> Triple(types, range, email) }
                .collect { (types, range, email) ->
                    _uiState.update { it.copy(
                        selectedTypes      = types,
                        timeRange          = range,
                        googleAccountEmail = email,
                    )}
                }
        }

        if (sdkStatus == SdkStatus.AVAILABLE) refreshPermissions()
    }

    // ── Permissions ───────────────────────────────────────────────────────

    fun refreshPermissions() {
        viewModelScope.launch {
            val granted = healthConnectRepository.getGrantedPermissions()
            _uiState.update { it.copy(grantedPermissions = granted) }
        }
    }

    fun onPermissionsResult(granted: Set<String>) =
        _uiState.update { it.copy(grantedPermissions = granted) }

    fun requiredPermissions(): Set<String> =
        healthConnectRepository.requiredPermissionsFor(HealthRecordType.all)

    fun missingPermissions(): Set<String> =
        healthConnectRepository.missingPermissions(
            HealthRecordType.all, _uiState.value.grantedPermissions)

    // ── Step 1 ────────────────────────────────────────────────────────────

    fun toggleType(type: HealthRecordType) {
        val cur = _uiState.value.selectedTypes
        updateSelectedTypes(if (type in cur) cur - type else cur + type)
    }

    fun setAllInCategory(category: RecordCategory, selected: Boolean) {
        val types = HealthRecordType.byCategory[category] ?: return
        val cur   = _uiState.value.selectedTypes
        updateSelectedTypes(if (selected) cur + types else cur - types.toSet())
    }

    fun selectAll()   = updateSelectedTypes(HealthRecordType.all.toSet())
    fun deselectAll() = updateSelectedTypes(emptySet())

    private fun updateSelectedTypes(types: Set<HealthRecordType>) {
        _uiState.update { it.copy(selectedTypes = types) }
        viewModelScope.launch { preferencesRepository.saveSelectedTypes(types) }
    }

    // ── Step 2 ────────────────────────────────────────────────────────────

    fun setTimeRange(range: TimeRange) {
        _uiState.update { it.copy(timeRange = range) }
        viewModelScope.launch { preferencesRepository.saveTimeRange(range) }
    }

    // ── Step 3 ────────────────────────────────────────────────────────────

    fun onSignInSuccess(email: String, displayName: String?) {
        _uiState.update { it.copy(googleAccountEmail = email, googleAccountName = displayName) }
        viewModelScope.launch { preferencesRepository.saveGoogleAccountEmail(email) }
    }

    fun signOut() {
        _uiState.update { it.copy(googleAccountEmail = null, googleAccountName = null) }
        viewModelScope.launch { preferencesRepository.saveGoogleAccountEmail(null) }
    }

    fun setSpreadsheetMode(mode: SpreadsheetMode) =
        _uiState.update { it.copy(spreadsheetMode = mode) }

    fun setSpreadsheetName(name: String) =
        _uiState.update { it.copy(spreadsheetName = name) }

    fun setSpreadsheetId(input: String) {
        val parsed = Regex("/spreadsheets/d/([a-zA-Z0-9_-]+)")
            .find(input)?.groupValues?.getOrNull(1) ?: input.trim()
        _uiState.update { it.copy(spreadsheetId = parsed) }
    }

    fun setExportMode(mode: ExportMode)     = _uiState.update { it.copy(exportMode = mode) }
    fun setScheduleType(type: ScheduleType) = _uiState.update { it.copy(scheduleType = type) }

    // ── Step 4 — export ───────────────────────────────────────────────────

    fun startExport() {
        val state = _uiState.value
        val email = state.googleAccountEmail ?: return
        if (state.selectedTypes.isEmpty()) return

        viewModelScope.launch {
            val spreadsheetId = resolveSpreadsheetId(state, email) ?: return@launch
            _uiState.update { it.copy(spreadsheetId = spreadsheetId) }
            runExport(state, spreadsheetId, email)
        }
    }

    fun resetExportState() = _uiState.update { it.copy(exportState = ExportState.Idle) }

    private suspend fun resolveSpreadsheetId(state: WizardUiState, email: String): String? {
        return try {
            when (state.spreadsheetMode) {
                SpreadsheetMode.CREATE_NEW ->
                    sheetsRepository.createSpreadsheet(state.spreadsheetName, email)
                SpreadsheetMode.USE_EXISTING ->
                    state.spreadsheetId?.takeIf { it.isNotBlank() }
                        ?: run {
                            _uiState.update { it.copy(
                                exportState = ExportState.Error("Nessun ID foglio specificato")) }
                            null
                        }
            }
        } catch (e: UserRecoverableAuthIOException) {
            _events.emit(WizardEvent.SheetsAuthRequired(e.intent))
            null
        } catch (e: Exception) {
            _uiState.update { it.copy(
                exportState = ExportState.Error(e.message ?: "Errore creazione foglio")) }
            null
        }
    }

    private suspend fun runExport(state: WizardUiState, spreadsheetId: String, email: String) {
        try {
            val types        = state.selectedTypes.toList()
            var totalRows    = 0
            var skippedTypes = 0

            // ── Phase 1: create all missing tabs in one API call ──────────
            _uiState.update { it.copy(exportState = ExportState.InProgress(
                currentType  = "Preparazione fogli…",
                currentIndex = 0,
                totalTypes   = types.size,
            ))}

            sheetsRepository.batchEnsureSheets(
                spreadsheetId = spreadsheetId,
                tabs          = types.map { it.sheetTabName to RecordSchema.forType(it).columns },
                accountEmail  = email,
            )

            // ── Phase 2: per-type read → filter → write ───────────────────
            types.forEachIndexed { index, type ->
                _uiState.update { it.copy(exportState = ExportState.InProgress(
                    currentType  = type.displayName,
                    currentIndex = index,
                    totalTypes   = types.size,
                ))}

                val schema  = RecordSchema.forType(type)
                val records = healthConnectRepository.readRecordsForType(type, state.timeRange)
                var rows    = records.flatMap { schema.extractRows(it) }

                // Smart append: discard rows already in the sheet
                if (state.exportMode == ExportMode.APPEND && rows.isNotEmpty()) {
                    val lastTs = sheetsRepository.getLastTimestamp(
                        spreadsheetId, type.sheetTabName, email)
                    if (lastTs != null) {
                        rows = rows.filter { row ->
                            (row.firstOrNull() as? String)?.let { it > lastTs } ?: true
                        }
                    }
                }

                _uiState.update { it.copy(exportState = ExportState.InProgress(
                    currentType  = type.displayName,
                    currentIndex = index,
                    totalTypes   = types.size,
                    recordCount  = rows.size,
                ))}

                if (rows.isEmpty()) {
                    skippedTypes++
                } else {
                    when (state.exportMode) {
                        ExportMode.OVERWRITE ->
                            sheetsRepository.overwriteSheetData(
                                spreadsheetId, type.sheetTabName, schema.columns, rows, email)
                        ExportMode.APPEND ->
                            sheetsRepository.appendRows(
                                spreadsheetId, type.sheetTabName, rows, email)
                    }
                    totalRows += rows.size
                }

                // Courtesy delay: ~400 ms keeps us well under 60 write-req/min
                if (index < types.lastIndex) delay(400L)
            }

            _uiState.update { it.copy(exportState = ExportState.Success(
                totalRows     = totalRows,
                skippedTypes  = skippedTypes,
                spreadsheetId = spreadsheetId,
            ))}

        } catch (e: UserRecoverableAuthIOException) {
            _events.emit(WizardEvent.SheetsAuthRequired(e.intent))
            _uiState.update { it.copy(exportState = ExportState.Idle) }
        } catch (e: Exception) {
            _uiState.update { it.copy(exportState =
                ExportState.Error(e.message ?: "Errore durante l'export")) }
        }
    }
}
