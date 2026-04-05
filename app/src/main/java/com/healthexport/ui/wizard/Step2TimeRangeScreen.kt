package com.healthexport.ui.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthexport.data.healthconnect.TimeRange
import com.healthexport.ui.components.WizardScaffold
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun Step2TimeRangeScreen(
    viewModel: WizardViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentRange = uiState.timeRange

    // Preset options shown as chips
    val presets = listOf(
        TimeRange.Last24Hours,
        TimeRange.LastWeek,
        TimeRange.LastMonth,
    )

    // Tracks whether the custom picker section is expanded
    val isCustom = currentRange is TimeRange.Custom
    var showPicker by remember { mutableStateOf(isCustom) }

    // DateRangePicker state — pre-populated from the current custom range if present
    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = when (currentRange) {
            is TimeRange.Custom -> currentRange.startDate
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            else -> Instant.now().minusSeconds(7 * 24 * 3600).toEpochMilli()
        },
        initialSelectedEndDateMillis = when (currentRange) {
            is TimeRange.Custom -> currentRange.endDate
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            else -> Instant.now().toEpochMilli()
        },
        // Prevent selecting dates in the future or older than 30 days (HC limit)
        selectableDates = PastThirtyDaysSelectableDates,
    )

    // Keep ViewModel in sync whenever the picker selection changes
    if (showPicker) {
        val start = pickerState.selectedStartDateMillis
        val end   = pickerState.selectedEndDateMillis
        if (start != null && end != null) {
            val startDate = Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalDate()
            val endDate   = Instant.ofEpochMilli(end).atZone(ZoneId.systemDefault()).toLocalDate()
            viewModel.setTimeRange(TimeRange.Custom(startDate, endDate))
        }
    }

    WizardScaffold(
        currentStep = 2,
        title       = "Intervallo temporale",
        subtitle    = "Quanti dati vuoi esportare?",
        onBack      = onBack,
        onNext      = onNext,
        nextEnabled = !isCustom ||
                (pickerState.selectedStartDateMillis != null &&
                 pickerState.selectedEndDateMillis   != null),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Text(
                text  = "Intervallo predefinito",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            // ── Preset chips ─────────────────────────────────────────────
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { preset ->
                    FilterChip(
                        selected = currentRange == preset,
                        onClick  = {
                            showPicker = false
                            viewModel.setTimeRange(preset)
                        },
                        label    = { Text(preset.displayName) },
                    )
                }
                // Custom chip
                FilterChip(
                    selected = isCustom,
                    onClick  = {
                        showPicker = true
                        // Immediately reflect a custom range in state
                        val start = pickerState.selectedStartDateMillis
                        val end   = pickerState.selectedEndDateMillis
                        if (start != null && end != null) {
                            val s = Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalDate()
                            val e = Instant.ofEpochMilli(end).atZone(ZoneId.systemDefault()).toLocalDate()
                            viewModel.setTimeRange(TimeRange.Custom(s, e))
                        }
                    },
                    label    = { Text("Personalizzato") },
                )
            }

            // ── Date range picker (shown only for custom) ─────────────────
            if (showPicker) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text  = "Seleziona il periodo",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                DateRangePicker(
                    state    = pickerState,
                    modifier = Modifier.fillMaxWidth(),
                    title    = null,
                    headline = null,
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Selectable dates constraint ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
private object PastThirtyDaysSelectableDates : androidx.compose.material3.SelectableDates {
    private val today      = LocalDate.now()
    private val thirtyAgo  = today.minusDays(30)

    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val date = Instant.ofEpochMilli(utcTimeMillis)
            .atZone(ZoneId.of("UTC"))
            .toLocalDate()
        return !date.isAfter(today) && !date.isBefore(thirtyAgo)
    }

    override fun isSelectableYear(year: Int): Boolean =
        year == today.year || year == thirtyAgo.year
}
