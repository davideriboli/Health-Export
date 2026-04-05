package com.healthexport.ui.wizard

import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthexport.data.healthconnect.HealthConnectRepository
import com.healthexport.data.healthconnect.HealthRecordType
import com.healthexport.data.healthconnect.RecordCategory
import com.healthexport.data.healthconnect.TimeRange
import com.healthexport.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI state ──────────────────────────────────────────────────────────────────

enum class SdkStatus { CHECKING, AVAILABLE, UNAVAILABLE, UPDATE_REQUIRED }

data class WizardUiState(
    val sdkStatus: SdkStatus                = SdkStatus.CHECKING,
    val grantedPermissions: Set<String>     = emptySet(),
    val selectedTypes: Set<HealthRecordType> = emptySet(),
    val timeRange: TimeRange                = TimeRange.LastWeek,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class WizardViewModel @Inject constructor(
    private val healthConnectRepository: HealthConnectRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WizardUiState())
    val uiState: StateFlow<WizardUiState> = _uiState.asStateFlow()

    init {
        // Resolve SDK availability immediately (synchronous call)
        val sdkStatus = when (healthConnectRepository.sdkStatus) {
            HealthConnectClient.SDK_AVAILABLE                          -> SdkStatus.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> SdkStatus.UPDATE_REQUIRED
            else                                                       -> SdkStatus.UNAVAILABLE
        }
        _uiState.update { it.copy(sdkStatus = sdkStatus) }

        // Load persisted preferences and merge with state
        viewModelScope.launch {
            combine(
                preferencesRepository.selectedTypesFlow,
                preferencesRepository.timeRangeFlow,
            ) { types, range -> types to range }
                .collect { (types, range) ->
                    _uiState.update { it.copy(selectedTypes = types, timeRange = range) }
                }
        }

        // Check which HC permissions are already granted
        if (sdkStatus == SdkStatus.AVAILABLE) refreshPermissions()
    }

    // ── Permissions ───────────────────────────────────────────────────────

    fun refreshPermissions() {
        viewModelScope.launch {
            val granted = healthConnectRepository.getGrantedPermissions()
            _uiState.update { it.copy(grantedPermissions = granted) }
        }
    }

    /** Called by the composable after the HC permission-request Activity returns. */
    fun onPermissionsResult(granted: Set<String>) {
        _uiState.update { it.copy(grantedPermissions = granted) }
    }

    fun requiredPermissions(): Set<String> =
        healthConnectRepository.requiredPermissionsFor(HealthRecordType.all)

    fun missingPermissions(): Set<String> =
        healthConnectRepository.missingPermissions(
            HealthRecordType.all,
            _uiState.value.grantedPermissions,
        )

    // ── Type selection ────────────────────────────────────────────────────

    fun toggleType(type: HealthRecordType) {
        val current = _uiState.value.selectedTypes
        val updated = if (type in current) current - type else current + type
        updateSelectedTypes(updated)
    }

    fun setAllInCategory(category: RecordCategory, selected: Boolean) {
        val categoryTypes = HealthRecordType.byCategory[category] ?: return
        val current = _uiState.value.selectedTypes
        val updated = if (selected) current + categoryTypes else current - categoryTypes.toSet()
        updateSelectedTypes(updated)
    }

    fun selectAll() = updateSelectedTypes(HealthRecordType.all.toSet())
    fun deselectAll() = updateSelectedTypes(emptySet())

    private fun updateSelectedTypes(types: Set<HealthRecordType>) {
        _uiState.update { it.copy(selectedTypes = types) }
        viewModelScope.launch { preferencesRepository.saveSelectedTypes(types) }
    }

    // ── Time range ────────────────────────────────────────────────────────

    fun setTimeRange(range: TimeRange) {
        _uiState.update { it.copy(timeRange = range) }
        viewModelScope.launch { preferencesRepository.saveTimeRange(range) }
    }
}
